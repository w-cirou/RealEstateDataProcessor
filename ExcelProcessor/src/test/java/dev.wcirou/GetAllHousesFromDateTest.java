package dev.wcirou;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.mockito.Mockito.mock;

//public class GetAllHousesFromDateTest {
//    @Mock
//    APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
//    @Mock
//    Context context;
//    @Mock
//    LambdaLogger logger;
//    @InjectMocks
//    GetAllHousesFromDate handler;
//    @BeforeEach
//    public void initialize_context_logger_and_handler() {
//        context = mock(Context.class);
//        logger = mock(LambdaLogger.class);
//        handler = new GetAllHousesFromDate();
//    }
//    @Test
//    public void getAllHousesFromDateTest() {
//        input.setPathParameters(Map.of("Date","2024-08-14"));
//        System.out.println(handler.handleRequest(input, context).getBody());
//    }
//}
