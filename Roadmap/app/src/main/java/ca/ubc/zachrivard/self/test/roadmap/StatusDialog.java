package ca.ubc.zachrivard.self.test.roadmap;


import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class StatusDialog extends Dialog {

    public Activity c;


    private ProgressBar spinner;
    private TextView header;


    private String headerMessage;

    public StatusDialog(Activity a, String headerMessage) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        this.headerMessage = headerMessage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.status_popup);

        spinner = (ProgressBar) findViewById(R.id.progress_bar);

        header = (TextView) findViewById(R.id.header_message);
        header.setText(headerMessage);
    }


    public void setHeaderMessage(String message){
        this.headerMessage = message;
        header.setText(headerMessage);
    }
}
