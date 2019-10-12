package iansteph.nhlp3.scheduler.handler;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.google.common.collect.ImmutableList;
import iansteph.nhlp3.scheduler.client.NhlClient;
import iansteph.nhlp3.scheduler.model.dynamo.NhlPlayByPlayProcessingItem;
import iansteph.nhlp3.scheduler.model.scheduler.Date;
import iansteph.nhlp3.scheduler.model.scheduler.Game;
import iansteph.nhlp3.scheduler.model.scheduler.ScheduleResponse;
import iansteph.nhlp3.scheduler.proxy.NhlProxy;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;

import java.time.ZonedDateTime;

public class SchedulerHandlerTest {

    private NhlClient mockNhlClient = mock(NhlClient.class);
    private NhlProxy mockNhlProxy = new NhlProxy(mockNhlClient);
    private CloudWatchEventsClient mockCloudWatchEventsClient = mock(CloudWatchEventsClient.class);
    private DynamoDBMapper mockDynamoDbMapper = mock(DynamoDBMapper.class);

    @Before
    public void setMockNhlClient() {
        final Game game1 = new Game();
        game1.setGameDate(ZonedDateTime.now());
        game1.setGamePk(0);

        final Date date1 = new Date();
        date1.setGames(ImmutableList.of(game1));

        final ScheduleResponse scheduleResponse = new ScheduleResponse();
        scheduleResponse.setDates(ImmutableList.of(date1));

        when(mockNhlClient.getScheduleForDate(any())).thenReturn(scheduleResponse);
        when(mockCloudWatchEventsClient.putRule(any(PutRuleRequest.class))).thenReturn(PutRuleResponse.builder().build());
        when(mockCloudWatchEventsClient.putTargets(any(PutTargetsRequest.class))).thenReturn(PutTargetsResponse.builder().build());
    }

    @Test
    public void successfulResponse() {
        final SchedulerHandler schedulerHandler = new SchedulerHandler(mockNhlProxy, mockCloudWatchEventsClient, mockDynamoDbMapper);
        final Object result = schedulerHandler.handleRequest(null, null);
        assertNotNull(result);
    }
}
