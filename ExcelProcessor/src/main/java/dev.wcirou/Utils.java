package dev.wcirou;

import com.google.gson.Gson;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

public class Utils {
    //Takes in an CSV file from MLS as an input stream and then inputs the data from it into DynamoDB
    public boolean processCsvFile(DynamoDbClient ddb, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
            //Looping through CSVRecords except the first one since that only contains the cell titles
            int count = 0;
            for (CSVRecord csvRecord : csvParser) {
                if(count!=0){
                    //Inserting the data from each cell as attributes in separate items, with one house per item
                    String[] cells = csvRecord.values();
                    Map<String, AttributeValue> item = new HashMap<>();
                    item.put("MLS#", AttributeValue.builder().s(cells[1]).build());
                    item.put("SubType", AttributeValue.builder().s(cells[2]).build());
                    item.put("CurrentPrice", AttributeValue.builder().s(cells[4]).build());
                    item.put("ListingType", AttributeValue.builder().s(cells[5].split("&")[1]).build());
                    item.put("Street", AttributeValue.builder().s(cells[6]).build());
                    item.put("Address", AttributeValue.builder().s(cells[7]).build());
                    item.put("SubdivisionName", AttributeValue.builder().s(cells[8]).build());
                    item.put("City", AttributeValue.builder().s(cells[9]).build());
                    item.put("County", AttributeValue.builder().s(cells[10]).build());
                    item.put("BedroomsTotal", AttributeValue.builder().s(cells[11]).build());
                    item.put("BedroomsFull", AttributeValue.builder().s(cells[12]).build());
                    item.put("Bedroom Half", AttributeValue.builder().s(cells[13]).build());
                    item.put("Year Built", AttributeValue.builder().s(cells[14]).build());
                    item.put("DOM", AttributeValue.builder().s(cells[15]).build());
                    item.put("Close Date", AttributeValue.builder().s(cells[16]).build());
                    item.put("DateAdded", AttributeValue.builder().s(LocalDate.now().toString()).build());
                    ddb.putItem(PutItemRequest.builder().tableName(System.getenv("TABLE_NAME")).item(item).build());
                }
                count++;
            }
            //If no exception was thrown return true to indicate success
            return true;
        } catch (Exception e) {
            System.out.println("Exception in processCsvFile method of type Exception: "+e.getMessage());
            //If exception was thrown return false to indicate not successful
            return false;
        }
    }
    public Map<String, AttributeValue> activeQueryByAddress(DynamoDbClient ddb, String address) {
        // Create the key condition expression
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":partitionKeyValue", AttributeValue.builder().s("gtActive").build());
        expressionAttributeValues.put(":lsiSortKeyValue", AttributeValue.builder().s(address).build());

        String keyConditionExpression = "ListingType = :partitionKeyValue and Address = :lsiSortKeyValue";

        // Create the QueryRequest
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(System.getenv("TABLE_NAME"))
                .keyConditionExpression(keyConditionExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .indexName("LSI") // Replace with your LSI index name
                .build();

        // Query the table and return items
        return ddb.query(queryRequest).items().get(0);
    }
    public Map<String, AttributeValue> activeQueryByMLSNum(DynamoDbClient ddb, String MLSNum) {
        Map<String, AttributeValue> key =  new HashMap<>();
        key.put("ListingType", AttributeValue.builder().s("gtActive").build());
        key.put("MLS#", AttributeValue.builder().s(MLSNum).build());
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(System.getenv("TABLE_NAME"))
                .key(key)
                .build();

        // Query the table and return items
        return ddb.getItem(getItemRequest).item();
    }
    public Map<String, AttributeValue> soldQueryByMLSNum (DynamoDbClient ddb, String MLSNum){
        Map<String, AttributeValue> key =  new HashMap<>();
        key.put("ListingType", AttributeValue.builder().s("gtSold").build());
        key.put("MLS#", AttributeValue.builder().s(MLSNum).build());
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(System.getenv("TABLE_NAME"))
                .key(key)
                .build();
        // Query the table and return items
        return ddb.getItem(getItemRequest).item();
    }
    public Map<String, AttributeValue> soldQueryByAddress (DynamoDbClient ddb, String address){
        // Create the key condition expression
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":partitionKeyValue", AttributeValue.builder().s("gtSold").build());
        expressionAttributeValues.put(":lsiSortKeyValue", AttributeValue.builder().s(address).build());

        String keyConditionExpression = "ListingType = :partitionKeyValue and Address = :lsiSortKeyValue";

        // Create the QueryRequest
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(System.getenv("TABLE_NAME"))
                .keyConditionExpression(keyConditionExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .indexName("LSI") // Replace with your LSI index name
                .build();
        // Query the table and return items
        return ddb.query(queryRequest).items().get(0);
    }
    public String getListingTypeByMLSNum(DynamoDbClient ddb, String MLSNum){
        List<Map<String,AttributeValue>> scanResponse = ddb.scan(ScanRequest.builder().tableName(System.getenv("TABLE_NAME")).build()).items();
        for (Map<String,AttributeValue> item : scanResponse){
            if (item.get("MLS#").s().equals(MLSNum)){
               return item.get("ListingType").s().replace("gt","");
            }
        }
        return "Not Found";
    }
    public String getListingTypeByAddress(DynamoDbClient ddb, String address){
        List<Map<String,AttributeValue>> scanResponse = ddb.scan(ScanRequest.builder().tableName(System.getenv("TABLE_NAME")).build()).items();
        for (Map<String,AttributeValue> item : scanResponse){
            if (item.get("Address").s().equals(address)){
                return item.get("ListingType").s().replace("gt","");
            }
        }
        return "Not Found";
    }
    public Map<String,String> getLastSevenDates(int day, int month, int year){
        Map<String, String> lastSevenDates = new HashMap<>();
        boolean keepGoing = true;
        int dayCount = day;
        int monthCount = month;
        int count = 1;
        while (keepGoing) {
            if (count == 7) {
                keepGoing = false;
            }
            if (monthCount == 0) {
                monthCount = 12;
            }
            if (dayCount == 0) {
                dayCount = 31;
                monthCount--;
            }
            if (monthCount<10 && dayCount>=10){
                String monthString = "0"+monthCount;
                lastSevenDates.put(count + ":", year + "-" + monthString + "-" + dayCount);
            }
            else if(dayCount<10 && monthCount>=10){
                String dayString = "0"+dayCount;
                lastSevenDates.put(count + ":", year + "-" + monthCount + "-" + dayString);
            }else {
                lastSevenDates.put(count + ":", year + "-" + monthCount + "-" + dayCount);
            }
            dayCount--;

            count++;
        }
        return lastSevenDates;
    }
    public Map<String,String> getLastThirtyDates(int day, int month, int year){
        Map<String, String> lastThirtyDates = new HashMap<>();
        boolean keepGoing = true;
        int dayCount = day;
        int monthCount = month;
        int count = 1;
        while (keepGoing) {
            if (count == 30) {
                keepGoing = false;
            }
            if (monthCount == 0) {
                monthCount = 12;
            }
            if (dayCount == 0) {
                dayCount = 31;
                monthCount--;
            }
            if (monthCount<10 && dayCount>=10){
                String monthString = "0"+monthCount;
                lastThirtyDates.put(count + ":", year + "-" + monthString + "-" + dayCount);
            }
            else if(dayCount<10 && monthCount>=10){
                String dayString = "0"+dayCount;
                lastThirtyDates.put(count + ":", year + "-" + monthCount + "-" + dayString);
            }else {
                lastThirtyDates.put(count + ":", year + "-" + monthCount + "-" + dayCount);
            }
            dayCount--;

            count++;
        }
        return lastThirtyDates;
    }
    public boolean sendEmail(SesClient sesClient, String sender, String recipient, String subject, String bodyText, String bodyHtml) {
        Content subjectContent = Content.builder().data(subject).build();
        Content textBody = Content.builder().data(bodyText).build();
        Content htmlBody = Content.builder().data(bodyHtml).build();
        Body body = Body.builder().text(textBody).html(htmlBody).build();

        Message message = Message.builder()
                .subject(subjectContent)
                .body(body)
                .build();

        SendEmailRequest request = SendEmailRequest.builder()
                .source(sender)
                .destination(Destination.builder().toAddresses(recipient).build())
                .message(message)
                .build();

        return sesClient.sendEmail(request).sdkHttpResponse().isSuccessful();
    }
    public double calcPriceAverage(ArrayList<Double> housePrices) {
        double priceTotal = 0;
        for (double soldPrice : housePrices) {
            priceTotal += soldPrice;
        }
        return priceTotal / housePrices.size();
    }
    public String formatItem(Gson gson, Map<String, AttributeValue> item){
        Map<String,String> itemMap = new HashMap<>();
        for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
            itemMap.put(entry.getKey().replace("'",""),entry.getValue().s().replace("'",""));
        }
        return gson.toJson(itemMap);
    }

}
