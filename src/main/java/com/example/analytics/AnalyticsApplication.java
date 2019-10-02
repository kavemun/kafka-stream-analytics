package com.example.analytics;

import lombok.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.streams.kstream.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@EnableBinding(AnalyticsBinding.class)
@SpringBootApplication
public class AnalyticsApplication {

	@Component
	public static class PageEventSource implements ApplicationRunner {

		private final MessageChannel pageViewsOut;
		private final Log log = LogFactory.getLog(getClass());
		public PageEventSource( AnalyticsBinding binding) {
			this.pageViewsOut = binding.pageViewsOut();
		}

		@Override
		public void run(ApplicationArguments args) throws Exception {

			List<String> names = Arrays.asList("jlong","pwebb","dwebb","jbourne");
			List<String> pages = Arrays.asList("blog","sitemap","initializr","news");

			Runnable runnable = () -> {
				String rPage = pages.get(new Random().nextInt(pages.size()));
				String rName = names.get(new Random().nextInt(names.size()));
				PageViewEvent pageViewEvent = new PageViewEvent(rName, rPage, (int)Math.random()*100, Math.random() > 5 ? 10: 1000);
				Message<PageViewEvent> message = MessageBuilder
						.withPayload(pageViewEvent)
						.setHeader(KafkaHeaders.MESSAGE_KEY, pageViewEvent.getUserId().getBytes())
						.build();
				try {
					this.pageViewsOut.send(message);
					log.info("sent " + message.toString() );
				}
				catch (Exception e){
					log.error(e);
				}
			};
			Executors.newScheduledThreadPool(1).scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
		}
	}

	@Component
	public static class PageViewEventProcessor{

		private final Log log = LogFactory.getLog(getClass());

		@StreamListener
		@SendTo(AnalyticsBinding.PRODUCER_OUT)
		public KStream<String, PageViewEvent> processEvent(
				@Input(AnalyticsBinding.CONSUMER_VIEWS_IN) KStream<String, PageViewEvent> events) {

//			return events
//					.filter((key, value) -> value.getDuration() > 10)
//					.mapValues((key, value) -> new KeyValue(value.getPage(), "0"))
//					.groupByKey()
////					.windowedBy(TimeWindows.of(1000 * 60))
//					.count(Materialized.as(AnalyticsBinding.PAGE_COUNT_MV))
//					.toStream();

			return events;

		}

	}

	@Component
	public static class PageCountSink {

		private final Log log = LogFactory.getLog(getClass());
		@StreamListener
		public void process(@Input((AnalyticsBinding.CONSUMER_IN)) KStream<String, PageViewEvent> event) {

//			event
//					.foreach((key,value) -> log.info("Test"));

		}
	}

	public static void main(String[] args) {
		SpringApplication.run(AnalyticsApplication.class, args);
	}

}

interface AnalyticsBinding {

	String PAGE_VIEWS_OUT = "pvout";
	String CONSUMER_VIEWS_IN = "pvin";
	String PAGE_COUNT_MV = "pcmv";
	String PRODUCER_OUT = "pcout" ;
	String CONSUMER_IN = "pcin" ;

	@Input(CONSUMER_VIEWS_IN)
	KStream<String, PageViewEvent> consumerViewsIn();

	@Output(PAGE_VIEWS_OUT)
	MessageChannel pageViewsOut();

	@Output(PRODUCER_OUT)
	KStream<String, PageViewEvent> producerOut();

	@Input(CONSUMER_IN)
	KStream<String, PageViewEvent> consumerIn();



}

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
class PageViewEvent {
	private String userId, page;
	private int randNum;
	private long duration;
}

//
//public class PageEventSerializer implements Serializer<PageViewEvent> {
//
//	private final ObjectMapper objectMapper = new ObjectMapper();
//
//	@Override
//	public void configure(Map<String, ?> configs, boolean isKey) {
//
//	}
//
//	@Override
//	public byte[] serialize(String topic, PageViewEvent data) {
//
//		if (Objects.isNull(data)) {
//			return null;
//		}
//		try {
//			return objectMapper.writeValueAsBytes(data);
//		} catch (Exception e) {
//			throw new SerializationException("Error serializing message", e);
//		}
//	}
//
//	@Override
//	public void close() {
//
//	}
//}
//
//public class PageEventDeserializer implements Deserializer<PageViewEvent> {
//
//	private ObjectMapper objectMapper = new ObjectMapper();
//
//	@Override
//	public void configure(Map<String, ?> configs, boolean isKey) {
//
//	}
//
//	@Override
//	public PageViewEvent deserialize(String topic, byte[] bytes) {
//		if (Objects.isNull(bytes)) {
//			return null;
//		}
//
//		PageViewEvent data;
//		try {
//			data = objectMapper.treeToValue(objectMapper.readTree(bytes), PageViewEvent.class);
//		} catch (Exception e) {
//			throw new SerializationException(e);
//		}
//
//		return data;
//	}
//
//	@Override
//	public void close() {
//
//	}
//}
//
//
