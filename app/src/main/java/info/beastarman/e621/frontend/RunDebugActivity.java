package info.beastarman.e621.frontend;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import info.beastarman.e621.middleware.NowhereToGoImageNavigator;
import info.beastarman.e621.middleware.OfflineImageNavigator;

public class RunDebugActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );

        Intent intent = new Intent(this,MainActivity.class);

        if(isDebuggable)
        {
			intent = new Intent(this,MainActivity.class);
        }

        startActivity(intent);

        finish();
    }
}
