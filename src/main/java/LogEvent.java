import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {
    AmazonDynamoDB dbClient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    DynamoDB dynamoDB = new DynamoDB(dbClient);

   public Object handleRequest(SNSEvent request, Context context) {

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        context.getLogger().log("Invocation started: " + timeStamp);

        context.getLogger().log("1: " + (request == null));

        context.getLogger().log("2: " + (request.getRecords().size()));

        context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());

        String to_mail = request.getRecords().get(0).getSNS().getMessage();

        String token = UUID.randomUUID().toString();
        String from_mail = "forgotPassword@prod.ankitpatro.me";
        String subject = "Forgot Password Link";
       String link = "http://ankitpatro.me/reset?email=" + to_mail + "&token=" + token;

       // The HTML body for the email.

       String HTMLBODY = "<p><a href='console.aws.amazon.com/console/home'>" +link+ "</a></p>";

        Table table = dynamoDB.getTable("snslambda");// update to tokenHolder
        Item item = table.getItem("username", to_mail); // update to username
        if (item == null) {
            context.getLogger().log("item is null");
            putItemInDynamo(context, to_mail, token, table);
            sendEmail(context, to_mail, from_mail, subject, link, HTMLBODY);

        } else {


            long ttlTime = (Long.parseLong(item.get("ttl").toString()));
            context.getLogger().log("ttlTime : "+ ttlTime);
            context.getLogger().log("difference between current time and ttl time"+ String.valueOf(ttlTime*1000-System.currentTimeMillis()));
            if(System.currentTimeMillis()>ttlTime*1000)
            {

                context.getLogger().log("item is not null and token is expired");
                putItemInDynamo(context, to_mail, token, table);
                sendEmail(context, to_mail, from_mail, subject, link, HTMLBODY);

            }


            context.getLogger().log("This record is already present in db and within ttl");

            return null;

        }



       context.getLogger().log("Invocation completed: " + timeStamp);
        return null;
    }

    private void sendEmail(Context context, String to_mail, String from_mail, String subject, String link, String htmlBody) {
        try {
            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1).build();

//            SendEmailRequest emailRequest = new SendEmailRequest()
//                    .withDestination(new Destination().withToAddresses(to_mail))
//                    .withMessage(new Message()
//                            .withBody(new Body().withText(new Content().withCharset("UTF-8").withData(link)))
//                            .withSubject(new Content().withCharset("UTF-8").withData(subject)))
//                    .withSource(from_mail);
            SendEmailRequest emailRequest = new SendEmailRequest()
                    .withDestination(new Destination().withToAddresses(to_mail))
                    .withMessage(new Message()
                            .withBody(new Body().withHtml(new Content().withCharset("UTF-8").withData(htmlBody)))
                            .withSubject(new Content().withCharset("UTF-8").withData(subject)))
                    .withSource(from_mail);
            SendEmailResult result = client.sendEmail(emailRequest);

            context.getLogger().log("Request result: " + result.toString());

        } catch (Exception e) {
            context.getLogger().log("Exception : " + e.getMessage());
        }
    }

    private void putItemInDynamo(Context context, String to_mail, String token, Table table) {
        context.getLogger().log("putItemInDynamo called");
        Item item;
        item = new Item().withPrimaryKey("username", to_mail).with("token", token).with("ttl",
                ((System.currentTimeMillis() / 1000 + 900))); // update to username and token with ttl of 15 mins

        PutItemOutcome outcome = table.putItem(item);

        context.getLogger().log("Put successful: " + outcome.toString());
    }
}
