package iansteph.nhlp3.scheduler.client;

import iansteph.nhlp3.scheduler.model.scheduler.ScheduleResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// This class is used for directly interacting with the NHL APIs. It handles the actual service calls made to the NHL API endpoints.
public class NhlClient {

    private final static RestTemplate restTemplate = new RestTemplate();
    private final static String BASE_NHL_URL = "http://statsapi.web.nhl.com/api/v1/";
    private final static String SCHEDULE_API = "schedule";

    public ScheduleResponse getScheduleForDate(final LocalDate date) {
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(BASE_NHL_URL + SCHEDULE_API)
                .queryParam("date", DateTimeFormatter.ISO_LOCAL_DATE.format(date));
        return restTemplate.getForObject(uriComponentsBuilder.toUriString(), ScheduleResponse.class);
    }
}
