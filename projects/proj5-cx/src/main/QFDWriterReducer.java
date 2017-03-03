import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.*;
import java.io.ObjectOutputStream;

public class QFDWriterReducer extends
    Reducer<WTRKey, RequestReplyMatch, NullWritable, NullWritable> {

    @Override
    public void reduce(WTRKey key,
                       Iterable<RequestReplyMatch> values,
                       Context ctxt)
            throws IOException, InterruptedException {
        Iterator<RequestReplyMatch> iterator = values.iterator();
        HashSet<RequestReplyMatch> set =
            new HashSet<RequestReplyMatch>();
        while (iterator.hasNext()){
            set.add(new RequestReplyMatch(iterator.next()));
        }
 
        String filename = "qfds/" + key.getName() + "/" +
                          key.getName() + "_" + key.getHashBytes();
        QueryFocusedDataSet data = new
            QueryFocusedDataSet(key.getName(),key.getHashBytes(), set);
        FileSystem system = FileSystem.get(ctxt.getConfiguration());
        FSDataOutputStream stream1 =
            system.create(new Path(filename), true);
        ObjectOutputStream stream2 = new ObjectOutputStream(stream1);
        stream2.writeObject(data);
        stream2.close();
        stream1.close();
    }
}
