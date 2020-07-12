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
import iansteph.nhlp3.scheduler.model.dynamo.ShiftPublishingItem;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class SchedulerHandlerTest extends UnitTestBase {

    private NhlClient mockNhlClient = mock(NhlClient.class);
    private NhlProxy mockNhlProxy = new NhlProxy(mockNhlClient);
    private CloudWatchEventsClient mockCloudWatchEventsClient = mock(CloudWatchEventsClient.class);
    private DynamoDBMapper mockDynamoDbMapper = mock(DynamoDBMapper.class);

    @Before
    public void setMockNhlClient() {

        when(mockNhlClient.getScheduleForDate(any(LocalDate.class))).thenReturn(generateScheduleResponseWithGame(generateGame()));
        when(mockCloudWatchEventsClient.putRule(any(PutRuleRequest.class))).thenReturn(PutRuleResponse.builder().build());
        when(mockCloudWatchEventsClient.putTargets(any(PutTargetsRequest.class))).thenReturn(PutTargetsResponse.builder().build());
    }

    @Test
    public void test_handleResponse_successfully_processes_response_when_there_are_scheduled_games() {

        final SchedulerHandler schedulerHandler = new SchedulerHandler(mockNhlProxy, mockCloudWatchEventsClient, mockDynamoDbMapper);
        final String expectedShiftPublishingItemKey = "SHIFTPUBLISHING-1";

        final Object result = schedulerHandler.handleRequest(null, null);

        assertThat(result, is(notNullValue()));
        final ArgumentCaptor<Object> dynamoDbMapperArgumentCaptor =
                ArgumentCaptor.forClass(Object.class);
        verify(mockDynamoDbMapper, times(2)).save(dynamoDbMapperArgumentCaptor.capture());
        final List<Object> invocationArguments = dynamoDbMapperArgumentCaptor.getAllValues();
        final NhlPlayByPlayProcessingItem item = (NhlPlayByPlayProcessingItem) invocationArguments.get(0);
        assertThat(item, is(notNullValue()));
        assertThat(item.getCompositeGameId(), is(notNullValue()));
        assertThat(item.getLastProcessedEventIndex(), is(0));
        final ShiftPublishingItem shiftPublishingItem = (ShiftPublishingItem) invocationArguments.get(1);
        assertThat(shiftPublishingItem, is(notNullValue()));
        assertThat(shiftPublishingItem.getPK(), is(expectedShiftPublishingItemKey));
        assertThat(shiftPublishingItem.getSK(), is(expectedShiftPublishingItemKey));
        assertThat(shiftPublishingItem.getShiftPublishingRecord(), is(notNullValue()));
        assertThat(shiftPublishingItem.getShiftPublishingRecord().size(), is(2));
        assertThat(shiftPublishingItem.getShiftPublishingRecord().get("visitor"), is(Collections.emptyMap()));
        assertThat(shiftPublishingItem.getShiftPublishingRecord().get("home"), is(Collections.emptyMap()));
    }

    @Test
    public void test_handleResponse_successfully_processes_response_when_there_are_postponed_games() {

        final ScheduleResponse scheduleResponse = generateScheduleResponseWithGame(generateGameWithDetailedState("Postponed"));
        when(mockNhlClient.getScheduleForDate(any(LocalDate.class))).thenReturn(scheduleResponse);
        final SchedulerHandler schedulerHandler = new SchedulerHandler(mockNhlProxy, mockCloudWatchEventsClient, mockDynamoDbMapper);
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
        final SchedulerHandler schedulerHandler = new SchedulerHandler(mockNhlProxy, mockCloudWatchEventsClient, mockDynamoDbMapper);
        final ArgumentCaptor<NhlPlayByPlayProcessingItem> dynamoDBMapperArgumentCaptor =
                ArgumentCaptor.forClass(NhlPlayByPlayProcessingItem.class);

        final Object result = schedulerHandler.handleRequest(null, null);

        assertThat(result, is(notNullValue()));
        verify(mockDynamoDbMapper, never()).save(dynamoDBMapperArgumentCaptor.capture());
    }
}
