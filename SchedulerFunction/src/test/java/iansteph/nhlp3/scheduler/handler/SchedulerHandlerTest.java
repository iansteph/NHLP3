package iansteph.nhlp3.scheduler.handler;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import iansteph.nhlp3.scheduler.UnitTestBase;
import iansteph.nhlp3.scheduler.client.NhlClient;
import iansteph.nhlp3.scheduler.model.dynamo.NhlPlayByPlayProcessingItem;
import iansteph.nhlp3.scheduler.model.scheduler.ScheduleResponse;
import iansteph.nhlp3.scheduler.proxy.NhlProxy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SchedulerHandlerTest extends UnitTestBase {

    private NhlClient mockNhlClient = mock(NhlClient.class);
    private NhlProxy mockNhlProxy = new NhlProxy(mockNhlClient);
    private CloudWatchEventsClient mockCloudWatchEventsClient = mock(CloudWatchEventsClient.class);
    private DynamoDBMapper mockDynamoDbMapper = mock(DynamoDBMapper.class);
    private DynamoDbClient mockDynamoDbClient = mock(DynamoDbClient.class);

    @Before
    public void setMockNhlClient() {

        when(mockNhlClient.getScheduleForDate(any(LocalDate.class))).thenReturn(generateScheduleResponseWithGame(generateGame()));
        when(mockCloudWatchEventsClient.putRule(any(PutRuleRequest.class))).thenReturn(PutRuleResponse.builder().build());
        when(mockCloudWatchEventsClient.putTargets(any(PutTargetsRequest.class))).thenReturn(PutTargetsResponse.builder().build());
    }

    @Test
    public void test_handleResponse_successfully_processes_response_when_there_are_scheduled_games() {

        final SchedulerHandler schedulerHandler = new SchedulerHandler(mockNhlProxy, mockCloudWatchEventsClient, mockDynamoDbMapper, mockDynamoDbClient);
        final String expectedShiftPublishingItemKey = "SHIFTPUBLISHING#1";

        final Object result = schedulerHandler.handleRequest(null, null);

        assertThat(result, is(notNullValue()));
        final ArgumentCaptor<NhlPlayByPlayProcessingItem> dynamoDbMapperArgumentCaptor =
                ArgumentCaptor.forClass(NhlPlayByPlayProcessingItem.class);
        verify(mockDynamoDbMapper, times(1)).save(dynamoDbMapperArgumentCaptor.capture());
        final NhlPlayByPlayProcessingItem item = dynamoDbMapperArgumentCaptor.getValue();
        assertThat(item, is(notNullValue()));
        assertThat(item.getCompositeGameId(), is(notNullValue()));
        assertThat(item.getLastProcessedEventIndex(), is(0));
        final ArgumentCaptor<PutItemRequest> putItemRequestArgumentCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockDynamoDbClient, times(1)).putItem(putItemRequestArgumentCaptor.capture());
        final PutItemRequest putItemRequest = putItemRequestArgumentCaptor.getValue();
        assertThat(putItemRequest.tableName(), is("NHLP3-Aggregate"));
        final Map<String, AttributeValue> actualItem = putItemRequest.item();
        assertThat(actualItem.size(), is(3));
        assertThat(actualItem.get("PK").s(), is(expectedShiftPublishingItemKey));
        assertThat(actualItem.get("SK").s(), is(expectedShiftPublishingItemKey));
        final Map<String, AttributeValue> actualShiftPublishingRecord = actualItem.get("shiftPublishingRecord").m();
        assertThat(actualShiftPublishingRecord.size(), is(2));
        assertThat(actualShiftPublishingRecord.get("visitor").m(), is(Collections.emptyMap()));
        assertThat(actualShiftPublishingRecord.get("home").m(), is(Collections.emptyMap()));
    }

    @Test
    public void test_handleResponse_successfully_processes_response_when_there_are_postponed_games() {

        final ScheduleResponse scheduleResponse = generateScheduleResponseWithGame(generateGameWithDetailedState("Postponed"));
        when(mockNhlClient.getScheduleForDate(any(LocalDate.class))).thenReturn(scheduleResponse);
        final SchedulerHandler schedulerHandler = new SchedulerHandler(mockNhlProxy, mockCloudWatchEventsClient, mockDynamoDbMapper, mockDynamoDbClient);
        final ArgumentCaptor<NhlPlayByPlayProcessingItem> dynamoDBMapperArgumentCaptor =
                ArgumentCaptor.forClass(NhlPlayByPlayProcessingItem.class);

        final Object result = schedulerHandler.handleRequest(null, null);

        assertThat(result, is(notNullValue()));
        verify(mockDynamoDbMapper, never()).save(dynamoDBMapperArgumentCaptor.capture());
    }

    @Test
    public void test_handleResponse_successfully_processes_response_when_there_are_no_games() {

        final ScheduleResponse scheduleResponse = new ScheduleResponse(Collections.emptyList());
        when(mockNhlClient.getScheduleForDate(any(LocalDate.class))).thenReturn(scheduleResponse);
        final SchedulerHandler schedulerHandler = new SchedulerHandler(mockNhlProxy, mockCloudWatchEventsClient, mockDynamoDbMapper, mockDynamoDbClient);
        final ArgumentCaptor<NhlPlayByPlayProcessingItem> dynamoDBMapperArgumentCaptor =
                ArgumentCaptor.forClass(NhlPlayByPlayProcessingItem.class);

        final Object result = schedulerHandler.handleRequest(null, null);

        assertThat(result, is(notNullValue()));
        verify(mockDynamoDbMapper, never()).save(dynamoDBMapperArgumentCaptor.capture());
    }
}
