package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class GetSoldHouse implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    //Getting Logger
    private static final Logger logger = LoggerFactory.getLogger(GetSoldHouse.class);
    //Creating Class Wide DynamoDB Client
    private static final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Utils Object to access methods
    private static final Utils utils = new Utils();
    //Creating Gson object
    private static final Gson gson = new Gson();
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            logger.info(input.getPath());

            //Checking for is the query is by address or MLS#, if both are included using MLS#
            Map<String, AttributeValue> item;
            if (input.getQueryStringParameters().get("Address") == null) {
                item = utils.soldQueryByMLSNum(ddb, input.getQueryStringParameters().get("MLSNum"));
            } else if (input.getQueryStringParameters().get("MLSNum") == null) {
                item = utils.soldQueryByAddress(ddb, input.getQueryStringParameters().get("Address"));
            } else {
                item = utils.soldQueryByMLSNum(ddb, input.getQueryStringParameters().get("MLSNum"));
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(utils.formatItem(gson,item));

        }catch(Exception e){
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Error of type Exception: "+e.getMessage());

        }
    }
}
