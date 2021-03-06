package com.postprocessor.Postprocessormicroservice;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class Postprocessor implements PostProcessService {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectid;

    @Value("${spring.cloud.gcp.topic-id-session-manager}")
    private String topicIdSessionManager;

    @Value("${spring.cloud.gcp.topic-id-api-manager}")
    private String topicIdApiManager;

    // @Async
    @Override
    @Async("processExecutor")
    public void getProcessedData(Map<String, Object> data) {
        try {

            Map<String, Object> data_map = (Map<String, Object>) data.get("data");

            Object sessionid_object = data_map.get("session_id");

            String sessionid1 = sessionid_object.toString();

            double sessionid2 = Double.parseDouble(sessionid1);

            int sessionid = (int) sessionid2;

            Map<String, Object> forecast = (Map<String, Object>) data_map.get("forecast");

            ArrayList<Map<String, Object>> forecasts = new ArrayList<Map<String, Object>>();

            int val = 1;

            while (true) {
                if (forecast.get(Integer.toString(val)) != null) {
                    forecasts.add((Map<String, Object>) forecast.get(Integer.toString(val)));
                    val++;
                } else {
                    break;
                }
            }

            String summary_string = "-----SUMMARY AFTER DATA ANALYSIS----- \n \n \n";

            summary_string = summary_string + "FORECAST SUMMARY:->";
            int val1 = 1;
            for (Map<String, Object> current : forecasts) {
                Map<String, Object> hourly_cur = (Map<String, Object>) current.get("hourly");
                Map<String, Object> daily_cur = (Map<String, Object>) current.get("daily");

                String hourly_data = hourly_cur.get("data").toString();
                String[] time_hourly = StringUtils.substringsBetween(hourly_data, "time=", ",");
                String[] summary_hourly = StringUtils.substringsBetween(hourly_data, "summary=", ",");

                String daily_data = daily_cur.get("data").toString();
                String[] time_daily = StringUtils.substringsBetween(daily_data, "time=", ",");
                String[] summary_daily = StringUtils.substringsBetween(daily_data, "summary=", ",");

                summary_string = summary_string + "\n \nFORECAST DAY " + val1 + " :-> \n \n";

                summary_string = summary_string + "HOURLY SUMMARY:-> \n \n";

                if (time_hourly != null && summary_hourly != null) {
                    for (int i = 0; i < time_hourly.length; i++) {
                        BigInteger bd = new BigDecimal(time_hourly[i]).toBigInteger();
                        int date_val = bd.intValue();
                        java.util.Date time = new java.util.Date((long) date_val * 1000);
                        String time_str = time.toString();
                        summary_string = summary_string + time_str + " : " + summary_hourly[i] + "\n";
                    }
                }

                summary_string = summary_string + "\nDAY SUMMARY:-> \n \n";

                if (time_daily != null && summary_daily != null) {
                    for (int i = 0; i < time_daily.length; i++) {
                        BigInteger bd = new BigDecimal(time_daily[i]).toBigInteger();
                        int date_val = bd.intValue();
                        java.util.Date time = new java.util.Date((long) date_val * 1000);
                        String time_str = time.toString();
                        summary_string = summary_string + time_str + " : " + summary_daily[i] + "\n";
                    }
                }

                val1++;
            }

            // This is for forecast_today.
            Map<String, Object> forecast_today = (Map<String, Object>) data_map.get("forecast_today");
            Map<String, Object> hourly = (Map<String, Object>) forecast_today.get("hourly");
            Map<String, Object> currently = (Map<String, Object>) forecast_today.get("currently");
            Map<String, Object> daily = (Map<String, Object>) forecast_today.get("daily");
            ArrayList<String> alerts = (ArrayList<String>) forecast_today.get("alerts");

            String hourly_data = hourly.get("data").toString();
            String[] time_hourly = StringUtils.substringsBetween(hourly_data, "time=", ",");
            String[] summary_hourly = StringUtils.substringsBetween(hourly_data, "summary=", ",");

            String currently_data = currently.toString();
            String[] time_currently = StringUtils.substringsBetween(currently_data, "time=", ",");
            String[] summary_currently = StringUtils.substringsBetween(currently_data, "summary=", ",");

            String daily_data = daily.get("data").toString();
            String[] time_daily = StringUtils.substringsBetween(daily_data, "time=", ",");
            String[] summary_daily = StringUtils.substringsBetween(daily_data, "summary=", ",");

            String alerts_data = "";

            if (alerts != null) {
                alerts_data = alerts.toString();
            }

            String[] alerts_title = StringUtils.substringsBetween(alerts_data, "title=", ",");
            String[] alerts_regions = StringUtils.substringsBetween(alerts_data, "regions=", "],");
            String[] alerts_severity = StringUtils.substringsBetween(alerts_data, "severity=", ",");
            String[] alerts_desc = StringUtils.substringsBetween(alerts_data, "description=", ", uri=");

            summary_string = summary_string + "CURRENT DAY SUMMARY:-> \n \n";

            if (time_currently != null && summary_currently != null) {
                for (int i = 0; i < time_currently.length; i++) {
                    BigInteger bd = new BigDecimal(time_currently[i]).toBigInteger();
                    int date_val = bd.intValue();
                    java.util.Date time = new java.util.Date((long) date_val * 1000);
                    String time_str = time.toString();
                    summary_string = summary_string + time_str + " : " + summary_currently[i] + "\n";
                }
            }

            summary_string = summary_string + "\nHOURLY SUMMARY:-> \n \n";

            if (time_hourly != null && summary_hourly != null) {
                for (int i = 0; i < time_hourly.length; i++) {
                    BigInteger bd = new BigDecimal(time_hourly[i]).toBigInteger();
                    int date_val = bd.intValue();
                    java.util.Date time = new java.util.Date((long) date_val * 1000);
                    String time_str = time.toString();
                    summary_string = summary_string + time_str + " : " + summary_hourly[i] + "\n";
                }
            }

            summary_string = summary_string + "\nDAILY SUMMARY:-> \n \n";

            if (time_daily != null && summary_daily != null) {
                for (int i = 0; i < time_daily.length; i++) {
                    BigInteger bd = new BigDecimal(time_daily[i]).toBigInteger();
                    int date_val = bd.intValue();
                    java.util.Date time = new java.util.Date((long) date_val * 1000);
                    String time_str = time.toString();
                    summary_string = summary_string + time_str + " : " + summary_daily[i] + "\n";
                }
            }

            summary_string = summary_string + "\nALERTS SUMMARY:-> \n \n";

            if (alerts_title != null
                    && alerts_regions != null
                    && alerts_severity != null
                    && alerts_desc != null) {
                for (int i = 0; i < alerts_title.length; i++) {
                    summary_string =
                            summary_string
                                    + alerts_title[i]
                                    + "\nSeverity:  "
                                    + alerts_severity[i]
                                    + "\nRegions affected: "
                                    + alerts_regions[i]
                                    + "\nDescription: "
                                    + alerts_desc[i]
                                    + "\n";
                }
            }

            summary_string = summary_string + "\n \n ---END OF SUMMARY---";

            // Creating pubsub responses.

            Map<String, Object> api_manager_message_for_pubsub = new HashMap<String, Object>();
            Map<String, Object> session_manager_message_for_pubsub = new HashMap<String, Object>();

            String status = "processed";

            api_manager_message_for_pubsub.put("session_id", sessionid);
            api_manager_message_for_pubsub.put("status", status);

            session_manager_message_for_pubsub.put("session_id", sessionid);
            session_manager_message_for_pubsub.put("status", status);
            session_manager_message_for_pubsub.put("processed_data", summary_string);

            Gson gson = new Gson();
            Type gsonType = new TypeToken<HashMap>() {
            }.getType();

            String api_manager_message_gson = gson.toJson(api_manager_message_for_pubsub, gsonType);
            String session_manager_message_gson =
                    gson.toJson(session_manager_message_for_pubsub, gsonType);

            System.out.println("Sending messages to pubsub");

            //String hostport = hostportvalue;
            //ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
            try {
/*
        TransportChannelProvider channelProvider =
            FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        // Set the channel and credentials provider when creating a `TopicAdminClient`.
        // Similarly for SubscriptionAdminClient
        TopicAdminClient topicClient =
            TopicAdminClient.create(
                TopicAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build());
*/
                ProjectTopicName topicNameForApiManager = ProjectTopicName.of(projectid, topicIdApiManager);
                ProjectTopicName topicNameForSessionManager =
                        ProjectTopicName.of(projectid, topicIdSessionManager);
                // Set the channel and credentials provider when creating a `Publisher`.
                // Similarly for Subscriber
                Publisher publisherForApiManager =
                        Publisher.newBuilder(topicNameForApiManager).build();

                Publisher publisherForSessionManager =
                        Publisher.newBuilder(topicNameForSessionManager).build();

                String topic = publisherForApiManager.getTopicNameString();

                String topic2 = publisherForSessionManager.getTopicNameString();

                ByteString data1 = ByteString.copyFromUtf8(api_manager_message_gson);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data1).build();
                ApiFuture<String> messageIdFuture = publisherForApiManager.publish(pubsubMessage);

                ApiFutures.addCallback(
                        messageIdFuture,
                        new ApiFutureCallback<String>() {
                            public void onSuccess(String messageId) {
                                System.out.println("published to API Manager with message id: " + messageId);
                            }

                            public void onFailure(Throwable t) {
                                System.out.println("failed to publish to API manager: " + t);
                            }
                        },
                        MoreExecutors.directExecutor());

                publisherForApiManager.shutdown();

                ByteString data2 = ByteString.copyFromUtf8(session_manager_message_gson);
                PubsubMessage pubsubMessage2 = PubsubMessage.newBuilder().setData(data2).build();
                ApiFuture<String> messageIdFuture2 = publisherForSessionManager.publish(pubsubMessage2);

                ApiFutures.addCallback(
                        messageIdFuture2,
                        new ApiFutureCallback<String>() {
                            public void onSuccess(String messageId) {
                                System.out.println("published to Session Manager with message id: " + messageId);
                            }

                            public void onFailure(Throwable t) {
                                System.out.println("failed to publish to Session Manager: " + t);
                            }
                        },
                        MoreExecutors.directExecutor());
                publisherForSessionManager.shutdown();

            } finally {
                // channel.shutdown();
            }

            System.out.println("Post processing complete!");
        } catch (Exception e) {
            System.out.println("Error in data post-processing.");
            e.printStackTrace();
        }
    }

    private Publisher getPublisher() {

        return null;
    }

    @Override
    public void getProcessedData() {
    }
}
