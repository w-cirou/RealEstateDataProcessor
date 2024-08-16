package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

public class InsertDataToDynamoDB implements RequestHandler<S3Event, String> {
    //Getting Logger
    private static final Logger logger = LoggerFactory.getLogger(InsertDataToDynamoDB.class);
    //Creating Utils object to access methods
    private final Utils utils = new Utils();
    //Creating Class Wide S3 Client
    private final S3Client s3 = S3Client.builder()
            .region(Region.US_EAST_1)
            .build();
    //Creating Class Wide DynamoDB Client
    private final DynamoDbClient ddb = DynamoDbClient
            .builder()
            .region(Region.US_EAST_1)
            .build();

    @Override
    public String handleRequest(S3Event input, Context context) {
        //Retrieving Bucket Name and File key from S3 event
        String BUCKET_NAME = input.getRecords().get(0).getS3().getBucket().getName();
        String FILE_KEY = input.getRecords().get(0).getS3().getObject().getKey();
        logger.info("Received S3 event: {}", input+"\n"+
                "Bucket Name: "+BUCKET_NAME+"\n" +
                "File Key: "+FILE_KEY);

        // Retrieving the data as Input Stream from S3
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(FILE_KEY)
                .build();

        InputStream inputStream = s3.getObject(getObjectRequest);

        // Processing data and inserting into DynamoDB
        boolean successful = utils.processCsvFile(ddb,inputStream);

        //Returning corresponding string based on success or failure of data insertion
        if(successful){
            logger.info("Data inserted successfully");
            return "Data inserted successfully";
        }else{
            logger.error("Failed to insert data");
            return "Failed to insert data";
        }
    }
}
