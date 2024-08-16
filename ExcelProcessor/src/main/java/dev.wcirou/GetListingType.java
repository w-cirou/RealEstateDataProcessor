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

public class GetListingType implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    //Getting Logger
    private static final Logger logger = LoggerFactory.getLogger(GetListingType.class);
    //Creating Class Wide DynamoDB Client
    private static final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Utils Object to access methods
    private static final Utils utils = new Utils();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            logger.info(input.getPath());

            //Checking for is the query is by address or MLS#, if both are included using MLS#
            String response;
            if (input.getQueryStringParameters().get("Address") == null) {
                response = utils.getListingTypeByMLSNum(ddb, input.getQueryStringParameters().get("MLSNum"));
            } else if (input.getQueryStringParameters().get("MLSNum") == null) {
                response = utils.getListingTypeByAddress(ddb, input.getQueryStringParameters().get("Address"));
            } else {
                response = utils.getListingTypeByMLSNum(ddb, input.getQueryStringParameters().get("MLSNum"));
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(response);

        }catch(Exception e){
            logger.error("Error of type Exception: {}", e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Error of type Exception: "+e.getMessage());
        }
    }
}
