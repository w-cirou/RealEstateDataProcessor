package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.ses.SesClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WeeklyInvoke implements RequestHandler<ScheduledEvent,Void> {
    //Creating Class Wide DynamoDB Client
    private final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Class Wide SES Client
    private final SesClient ses = SesClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Utils Object to access methods
    private static final Utils utils = new Utils();
    //Getting Today's Date
    private static final String DATE_TODAY = String.valueOf(LocalDate.now());
    //Getting Logger
    private static final Logger logger = LoggerFactory.getLogger(WeeklyInvoke.class);
    //Setting Sender and Recipient
    private static final String sender = "XXXXXXXXX";
    private static final String recipient = "XXXXXXXXX";

    @Override
    public Void handleRequest(ScheduledEvent scheduledEvent, Context context) {
        try {
            //Splitting Today's Date
            int year = Integer.parseInt(DATE_TODAY.split("-")[0]);
            int month = Integer.parseInt(DATE_TODAY.split("-")[1]);
            int day = Integer.parseInt(DATE_TODAY.split("-")[2]);

            //Getting dates from the last seven days
            Map<String, String> lastSevenDates = utils.getLastSevenDates(day, month, year);

            //Scanning Table
            List<Map<String, AttributeValue>> items = ddb.scan(scanRequest -> scanRequest.tableName(System.getenv("TABLE_NAME"))).items();

            //Creating Map of Strings with items from the last week
            Map<String, String> weekOldItems = new HashMap<>();
            for (Map<String, AttributeValue> item : items) {
                String itemDate = item.get("DateAdded").s();
                if (lastSevenDates.containsValue(itemDate)) {
                    weekOldItems.put(item.get("MLS#").s(), item.get("Address").s());
                }
            }

            //Getting Sold and Active House Prices
            ArrayList<Double> soldHousePrices = new ArrayList<>();
            ArrayList<Double> activeHousePrices = new ArrayList<>();
            for (Map<String, AttributeValue> item : items) {
                if (item.get("ListingType").s().equals("gtSold")) {
                    if (weekOldItems.containsValue(item.get("Address").s())) {
                        soldHousePrices.add(Double.parseDouble(item.get("CurrentPrice").s().replace("$", "").replace(",", "")));
                    }
                } else if (item.get("ListingType").s().equals("gtActive")) {
                    if (weekOldItems.containsValue(item.get("Address").s())) {
                        activeHousePrices.add(Double.parseDouble(item.get("CurrentPrice").s().replace("$", "").replace(",", "")));
                    }
                }
            }

            //Calculating Average Sold Prices
            double soldPriceAverage = utils.calcPriceAverage(soldHousePrices);
            logger.info("Average Sold House Price: " + soldPriceAverage);

            //Calculating Average Active Prices
            double activePriceAverage = utils.calcPriceAverage(activeHousePrices);
            logger.info("Average Active House Price: " + activePriceAverage);

            //Creating Message to be sent to Emailed
            String message = "Average Active Listing Price: $" + String.format("%,.2f", activePriceAverage)
                    + "<br>Average Sell Price: $" + String.format("%,.2f", soldPriceAverage);
            String bodyHtml = "<html><head></head><body><h1>Info From Last Week:</h1><p>" + message + "</p></body></html>";

            //Sending Email
            utils.sendEmail(ses, sender, recipient, "Weekly Update", message, bodyHtml);
            logger.info("Email Sent");
        }catch (Exception e){
            logger.error("Error of type Exception: "+e.getMessage());
        }
        return null;
    }
}
