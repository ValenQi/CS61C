import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.io.ObjectInputStream;

public class TotalFailMapper extends Mapper<LongWritable, Text, WTRKey,
                                            RequestReplyMatch> {
 
    private MessageDigest messageDigest;

    @Override
    public void setup(Context ctxt) throws IOException, InterruptedException {
        super.setup(ctxt);
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available");
        }
        messageDigest.update(HashUtils.SALT.
                             getBytes(StandardCharsets.UTF_8));
    }

    private QueryFocusedDataSet getQFDS(String type, String str, Context ctxt)
            throws IOException{
        String filename = "qfds/" + type + "/" + type + "_" + hash(str);
        FileSystem fileSys =
            FileSystem.get(ctxt.getConfiguration());
        FSDataInputStream dInputStream =
            fileSys.open(new Path(filename));
        ObjectInputStream oInputStream =
            new ObjectInputStream(dInputStream);
        try {
            QueryFocusedDataSet qfds =
                (QueryFocusedDataSet)oInputStream.readObject();
            return qfds;
        } catch(ClassNotFoundException e) {
            return null;
        }
    }
 
    private String hash(String str){
        MessageDigest md =
            HashUtils.cloneMessageDigest(messageDigest);
        md.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] hash = md.digest();
        byte[] hashBytes =
            Arrays.copyOf(hash, HashUtils.NUM_HASH_BYTES);
        String hashString =
            DatatypeConverter.printHexBinary(hashBytes);
        return hashString;
    }

    @Override
    public void map(LongWritable lineNo, Text line, Context ctxt)
            throws IOException, InterruptedException {

        QueryFocusedDataSet qfdsS =
            getQFDS("srcIP", line.toString(), ctxt);
        for (RequestReplyMatch match : qfdsS.getMatches()){
            QueryFocusedDataSet qfdsC =
                getQFDS("cookie", match.getCookie(), ctxt);
            for (RequestReplyMatch m : qfdsC.getMatches())
                ctxt.write(new WTRKey("torusers", 
                                      hash(m.getUserName())), m); 
        }
    }
}
