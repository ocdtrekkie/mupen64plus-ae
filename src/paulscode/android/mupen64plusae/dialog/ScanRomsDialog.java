package paulscode.android.mupen64plusae.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.util.FileUtil;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ScanRomsDialog implements OnClickListener, OnItemClickListener
{
    public interface ScanRomsDialogListener
    {
        /**
         * Called when the dialog is dismissed and should be used to process the file selected by
         * the user.
         * 
         * @param file The file selected by the user, or null if the user clicks the dialog's
         * negative button.
         * @param which The DialogInterface button pressed by the user.
         */
        public void onDialogClosed( File file, int which );
    }
    
    private final ScanRomsDialogListener mListener;
    private final List<CharSequence> mNames;
    private final List<String> mPaths;
    private final AlertDialog mDialog;
    private final File mStartPath;
    
    @SuppressLint( "InflateParams" )
    public ScanRomsDialog( Activity activity, File startPath, ScanRomsDialogListener listener )
    {
        mListener = listener;
        
        // Pick the root of the storage directory by default
        if( startPath == null || !startPath.exists() )
            startPath = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        mStartPath = startPath;
        
        // Get the filenames and absolute paths
        mNames = new ArrayList<CharSequence>();
        mPaths = new ArrayList<String>();
        FileUtil.populate( startPath, true, true, true, mNames, mPaths );
        
        // Inflate layout
        final LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View layout = inflater.inflate( R.layout.scan_roms_dialog, null );
        
        // Populate the file list
        ListView listView1 = (ListView) layout.findViewById( R.id.listView1 );
        ArrayAdapter<String> adapter = Prompt.createFilenameAdapter( activity, mPaths, mNames );
        listView1.setAdapter( adapter );
        listView1.setOnItemClickListener( this );
        
        // Create the dialog
        Builder builder = new Builder( activity ).setTitle( startPath.getPath() )
                .setCancelable( false ).setView( layout )
                .setPositiveButton( android.R.string.ok, this )
                .setNegativeButton( android.R.string.cancel, this );

        mDialog = builder.create();
    }
    
    public void show()
    {
        mDialog.show();
    }
    
    public void dismiss()
    {
        mDialog.dismiss();
    }

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        onClick( null, position );
    }
    
    @Override
    public void onClick( DialogInterface dlg, int which )
    {
        dismiss();
        
        if( which >= 0 && which < mNames.size() )
            mListener.onDialogClosed( new File( mPaths.get( which ) ), which );
        else if( which == DialogInterface.BUTTON_POSITIVE )
            mListener.onDialogClosed( mStartPath, which );
        else
            mListener.onDialogClosed( null, which );
    }
}
