import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.io.IntWritable;

import java.io.IOException;

public class QFDMatcherMapper extends Mapper<LongWritable, Text,
				      IntWritable, WebTrafficRecord> {

    @Override
    public void map(LongWritable lineNo, Text line, Context ctxt)
	        throws IOException, InterruptedException {
        WebTrafficRecord wtr = new WebTrafficRecord();
        wtr = WebTrafficRecord.parseFromLine(line.toString());
        int key = wtr.matchHashCode();
        ctxt.write(new IntWritable(key), wtr);
    }
}
