import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;

import java.io.IOException;
import java.util.*;

public class QFDMatcherReducer extends
    Reducer<IntWritable, WebTrafficRecord,RequestReplyMatch, NullWritable> {

    @Override
    public void reduce(IntWritable key,
                       Iterable<WebTrafficRecord> values,
                       Context ctxt)
            throws IOException, InterruptedException {
        Iterator<WebTrafficRecord> iterator = values.iterator();
        ArrayList<WebTrafficRecord> list =
            new ArrayList<WebTrafficRecord>();
        NullWritable nullWritable = NullWritable.get();
        while (iterator.hasNext()){
            list.add(new WebTrafficRecord(iterator.next()));
        }

        int i = 0;
        while (!list.isEmpty() && i < (list.size() - 1)){
            WebTrafficRecord request =
                new WebTrafficRecord(list.get(i));
            WebTrafficRecord reply = new WebTrafficRecord();
            int replyIndex = -1;
            for (int j = 1; j < list.size() && replyIndex == -1; j ++) {
                WebTrafficRecord currWTR = list.get(j);
                if (currWTR.tupleMatches(request) &&
                    Math.abs(request.getTimestamp() -
                        currWTR.getTimestamp()) <= 10){
                    reply = new WebTrafficRecord(currWTR);
                    replyIndex = j; 
                }
            }

            if (replyIndex != -1){
                RequestReplyMatch rrm;
                if (request.getUserName() == null){
                    rrm = new RequestReplyMatch(request, reply);
                } else {
                    rrm = new RequestReplyMatch(reply, request);
                }
                list.remove(replyIndex);
                list.remove(0);
                ctxt.write(rrm, nullWritable);
            } else {
                i ++;
            } 
        }
    }
}
