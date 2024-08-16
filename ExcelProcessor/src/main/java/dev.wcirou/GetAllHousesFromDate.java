package dev.wcirou;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import java.util.Map;

public class GetAllHousesFromDate implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    //Creating Class Wide DynamoDB Client
    private  final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Getting Logger
    private static final Logger logger = LoggerFactory.getLogger(GetAllHousesFromDate.class);
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            //Getting Date from Path Parameter
            String date = input.getPathParameters().get("Date");
            //Creating Response body
            String responseString="";
            for (Map<String, AttributeValue> item : ddb.scan(ScanRequest.builder().tableName(System.getenv("TABLE_NAME")).build()).items()) {
                if (item.get("DateAdded").s().equals(date)) {
                    responseString += item.get("Address").s()+"\n";
                }
            }
            logger.info(responseString);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseString);
        }catch(Exception e){
            logger.error("Error of type Exception: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Error of type Exception: "+e.getMessage());
        }
    }
}
