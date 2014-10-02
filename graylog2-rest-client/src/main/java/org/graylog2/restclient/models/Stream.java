/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ApiRequestBuilder;
import org.graylog2.restclient.models.alerts.Alert;
import org.graylog2.restclient.models.alerts.AlertCondition;
import org.graylog2.restclient.models.alerts.AlertConditionService;
import org.graylog2.restclient.models.api.requests.alerts.CreateAlertConditionRequest;
import org.graylog2.restclient.models.api.responses.alerts.AlertSummaryResponse;
import org.graylog2.restclient.models.api.responses.alerts.AlertsResponse;
import org.graylog2.restclient.models.api.responses.alerts.CheckConditionResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamRuleSummaryResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamSummaryResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamThroughputResponse;
import org.graylog2.restroutes.generated.routes;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class Stream {

    private final StreamService streamService;

    public interface Factory {
        public Stream fromSummaryResponse(StreamSummaryResponse ssr);
    }

    private final ApiClient api;
	
	private final String id;
    private final String title;
    private final String description;
    private final String creatorUserId;
    private final String createdAt;
    private final List<StreamRule> streamRules;
    private final Boolean disabled;

    private final UserService userService;
    private final AlertConditionService alertConditionService;
    private final StreamRule.Factory streamRuleFactory;

    private final List<String> userAlertReceivers;
    private final List<String> emailAlertReceivers;

    private AlertsResponse alertsResponse;

	@AssistedInject
    private Stream(ApiClient api,
                   UserService userService,
                   AlertConditionService alertConditionService,
                   StreamRule.Factory streamRuleFactory,
                   StreamService streamService,
                   @Assisted StreamSummaryResponse ssr) {
        this.streamService = streamService;
        this.id = ssr.id;
        this.title = ssr.title;
        this.description = ssr.description;
        this.creatorUserId = ssr.creatorUserId;
        this.createdAt = ssr.createdAt;

        this.streamRules = Lists.newArrayList();

        this.disabled = ssr.disabled;

        this.api = api;
        this.userService = userService;
        this.alertConditionService = alertConditionService;
        this.streamRuleFactory = streamRuleFactory;

        if (ssr.alertReceivers != null) {
            if (ssr.alertReceivers.containsKey("users") && ssr.alertReceivers.get("users") != null) {
                userAlertReceivers = ssr.alertReceivers.get("users");
            } else {
                userAlertReceivers = Lists.newArrayList();
            }

            if (ssr.alertReceivers.containsKey("emails") && ssr.alertReceivers.get("emails") != null) {
                emailAlertReceivers = ssr.alertReceivers.get("emails");
            } else {
                emailAlertReceivers = Lists.newArrayList();
            }
        } else {
            userAlertReceivers = Lists.newArrayList();
            emailAlertReceivers = Lists.newArrayList();
        }

        for (StreamRuleSummaryResponse streamRuleSummaryResponse : ssr.streamRules) {
            streamRules.add(streamRuleFactory.fromSummaryResponse(streamRuleSummaryResponse));
        }
	}

    public void addAlertCondition(CreateAlertConditionRequest r) throws APIException, IOException {
        alertConditionService.create(this, r);
    }

    public void addAlertReceiver(User user) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().addReceiver(getId()))
                .queryParam("entity", user.getName())
                .queryParam("type", "users")
                .expect(201)
                .execute();
    }

    public void addAlertReceiver(String email) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().addReceiver(getId()))
                .queryParam("entity", email)
                .queryParam("type", "emails")
                .expect(201)
                .execute();
    }

    public void removeAlertReceiver(User user) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().removeReceiver(getId()))
                .queryParam("entity", user.getName())
                .queryParam("type", "users")
                .expect(204)
                .execute();
    }

    public void removeAlertReceiver(String email) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().removeReceiver(getId()))
                .queryParam("entity", email)
                .queryParam("type", "emails")
                .expect(204)
                .execute();
    }

    public List<Alert> getAlerts() throws APIException, IOException {
        return getAlertsSince(0);
    }

    public List<Alert> getAlertsSince(int since) throws APIException, IOException {
        List<Alert> alerts = Lists.newArrayList();

        for (AlertSummaryResponse alert : getAlertsInformation(since).alerts) {
            alerts.add(new Alert(alert));
        }

        return alerts;
    }

    public Long getTotalAlerts() throws APIException, IOException {
        return getAlertsInformation(0).total;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public User getCreatorUser() {
        return userService.load(this.creatorUserId);
    }

    public DateTime getCreatedAt() {
        return DateTime.parse(createdAt);
    }

    public List<StreamRule> getStreamRules() {
        return streamRules;
    }

    public Boolean getDisabled() {
        return (disabled != null && disabled);
    }

    private AlertsResponse getAlertsInformation(int since) throws APIException, IOException {
        if (alertsResponse == null) {
            ApiRequestBuilder<AlertsResponse> call = api.path(routes.StreamAlertResource().list(getId()), AlertsResponse.class);

            if (since > 0) {
                call.queryParam("since", since);
            }

            alertsResponse = call.execute();
        }

        return alertsResponse;
    }

    public int getActiveAlerts() throws APIException, IOException {
        /*int total = 0;
        for (AlertCondition alertCondition : this.alertConditionService.allOfStream(this)) {
            if (alertCondition.getParameters() == null) continue;

            int time = (alertCondition.getParameters().get("time") == null ? 0 : Integer.parseInt(alertCondition.getParameters().get("time").toString()));
            int grace = (alertCondition.getParameters().get("grace") == null ? 0 : Integer.parseInt(alertCondition.getParameters().get("grace").toString()));
            int since = Math.round(new DateTime().minusMinutes((time + grace == 0 ? 1 : time + grace)).getMillis()/1000);
            total += getAlertsInformation(since).alerts.size();
        }*/
        CheckConditionResponse response = streamService.activeAlerts(this.getId());
        int size = (response.results == null ? 0 : response.results.size());

        return size;
    }

    public long getThroughput() throws APIException, IOException {
        final StreamThroughputResponse throughputResponse = api.path(routes.StreamResource().oneStreamThroughput(getId()), StreamThroughputResponse.class)
                .expect(200, 404)
                .execute();

        if (throughputResponse == null) {
            return 0L;
        }
        return throughputResponse.throughput;
    }


    public List<String> getUserAlertReceivers() {
        return userAlertReceivers;
    }

    public List<String> getEmailAlertReceivers() {
        return emailAlertReceivers;
    }

}
