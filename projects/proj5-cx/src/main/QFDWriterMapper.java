import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.xml.bind.DatatypeConverter;

public class QFDWriterMapper extends Mapper<RequestReplyMatch,
				     NullWritable, WTRKey,
				     RequestReplyMatch> {

    private MessageDigest messageDigest;

    @Override
    public void setup(Context ctxt)
            throws IOException, InterruptedException {
        super.setup(ctxt);
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available");
        }
        messageDigest.update(HashUtils.SALT.
                             getBytes(StandardCharsets.UTF_8));
    }

    @Override
	public void map(RequestReplyMatch record, NullWritable ignore, Context ctxt)
	        throws IOException, InterruptedException {
        MessageDigest srcIP =
            HashUtils.cloneMessageDigest(messageDigest);
        srcIP.update(record.getSrcIp().
                     getBytes(StandardCharsets.UTF_8));
        byte[] hashSrcIPBytes =
            Arrays.copyOf(srcIP.digest(), HashUtils.NUM_HASH_BYTES);
        String hashSrcIPString =
            DatatypeConverter.printHexBinary(hashSrcIPBytes);
        WTRKey srcIPKey = new WTRKey("srcIP", hashSrcIPString);
        ctxt.write(srcIPKey, record);

        MessageDigest destIP =
            HashUtils.cloneMessageDigest(messageDigest);
        destIP.update(record.getDestIp().
                      getBytes(StandardCharsets.UTF_8));
        byte[] hashDestIPBytes =
            Arrays.copyOf(destIP.digest(), HashUtils.NUM_HASH_BYTES);
        String hashDestIPString =
            DatatypeConverter.printHexBinary(hashDestIPBytes);
        WTRKey destIPKey = new WTRKey("destIP", hashDestIPString);
        ctxt.write(destIPKey, record);
 
        MessageDigest cookie =
            HashUtils.cloneMessageDigest(messageDigest);
        cookie.update(record.getCookie().
                      getBytes(StandardCharsets.UTF_8));
        byte[] hashCookieBytes =
            Arrays.copyOf(cookie.digest(), HashUtils.NUM_HASH_BYTES);
        String hashCookieString =
            DatatypeConverter.printHexBinary(hashCookieBytes);
        WTRKey cookieKey = new WTRKey("cookie", hashCookieString);
        ctxt.write(cookieKey, record);
    }
}
