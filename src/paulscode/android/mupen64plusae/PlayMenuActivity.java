/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.cheat.CheatEditorActivity;
import paulscode.android.mupen64plusae.cheat.CheatFile;
import paulscode.android.mupen64plusae.cheat.CheatFile.CheatSection;
import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.preference.ProfilePreference;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.bda.controller.Controller;

public class PlayMenuActivity extends PreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_CHEATS = "screenCheats";
    
    private static final String CATEGORY_GAME_SETTINGS = "categoryGameSettings";
    private static final String CATEGORY_CHEATS = "categoryCheats";
    
    public static final String ACTION_RESUME = "actionResume";
    public static final String ACTION_RESTART = "actionRestart";
    public static final String ACTION_CHEAT_EDITOR = "actionCheatEditor";
    public static final String ACTION_WIKI = "actionWiki";
    public static final String ACTION_RESET_GAME_PREFS = "actionResetGamePrefs";
    public static final String ACTION_EXIT = "actionExit";
    public static final String ACTION_GLOBAL_SETTINGS = "screenGlobalSettings";
    
    public static final String EMULATION_PROFILE = "emulationProfile";
    public static final String TOUCHSCREEN_PROFILE = "touchscreenProfile";
    public static final String CONTROLLER_PROFILE1 = "controllerProfile1";
    public static final String CONTROLLER_PROFILE2 = "controllerProfile2";
    public static final String CONTROLLER_PROFILE3 = "controllerProfile3";
    public static final String CONTROLLER_PROFILE4 = "controllerProfile4";
    public static final String PLAYER_MAP = "playerMap";
    public static final String PLAY_SHOW_CHEATS = "playShowCheats";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private GamePrefs mGamePrefs = null;
    private SharedPreferences mPrefs = null;
    
    // ROM info
    private String mRomPath = null;
    private String mRomMd5 = null;
    private RomHeader mRomHeader = null;
    private RomDatabase mRomDatabase = null;
    private RomDetail mRomDetail = null;
    
    // Preference menu items
    ProfilePreference mEmulationProfile = null;
    ProfilePreference mTouchscreenProfile = null;
    ProfilePreference mControllerProfile1 = null;
    ProfilePreference mControllerProfile2 = null;
    ProfilePreference mControllerProfile3 = null;
    ProfilePreference mControllerProfile4 = null;
    PreferenceGroup mScreenCheats = null;
    PreferenceGroup mCategoryCheats = null;
    
    // MOGA controller interface
    private Controller mMogaController = Controller.getInstance( this );
    
    // Go directly to gameplay from the gallery
    public static String action = null;
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get the ROM path and MD5 that was passed to the activity
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );
        mRomPath = extras.getString( Keys.Extras.ROM_PATH );
        mRomMd5 = extras.getString( Keys.Extras.ROM_MD5 );
        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( mRomMd5 ) )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );
        
        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mRomHeader = new RomHeader( mRomPath );
        mUserPrefs = new UserPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomHeader );
        mUserPrefs.enforceLocale( this );
        mPrefs = getSharedPreferences( mGamePrefs.sharedPrefsName, MODE_PRIVATE );
        
        // Get the detailed info about the ROM
        mRomDatabase = new RomDatabase( mAppData.mupen64plus_ini );
        mRomDetail = mRomDatabase.lookupByMd5WithFallback( mRomMd5, new File( mRomPath ) );
        
        // Load user preference menu structure from XML and update view
        getPreferenceManager().setSharedPreferencesName( mGamePrefs.sharedPrefsName );
        addPreferencesFromResource( R.xml.preferences_game );
        mEmulationProfile = (ProfilePreference) findPreference( EMULATION_PROFILE );
        mTouchscreenProfile = (ProfilePreference) findPreference( TOUCHSCREEN_PROFILE );
        mControllerProfile1 = (ProfilePreference) findPreference( CONTROLLER_PROFILE1 );
        mControllerProfile2 = (ProfilePreference) findPreference( CONTROLLER_PROFILE2 );
        mControllerProfile3 = (ProfilePreference) findPreference( CONTROLLER_PROFILE3 );
        mControllerProfile4 = (ProfilePreference) findPreference( CONTROLLER_PROFILE4 );
        mScreenCheats = (PreferenceGroup) findPreference( SCREEN_CHEATS );
        mCategoryCheats = (PreferenceGroup) findPreference( CATEGORY_CHEATS );
        
        // Set some game-specific strings
        setTitle( mRomDetail.goodName );
        if( !TextUtils.isEmpty( mRomDetail.baseName ) )
        {
            String title = getString( R.string.categoryGameSettings_titleNamed, mRomDetail.baseName );
            findPreference( CATEGORY_GAME_SETTINGS ).setTitle( title );
        }
        
        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESUME, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESTART, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_CHEAT_EDITOR, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_WIKI, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESET_GAME_PREFS, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_GLOBAL_SETTINGS, this );
        
        // Remove wiki menu item if not applicable
        if( TextUtils.isEmpty( mRomDetail.wikiUrl ) )
        {
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, ACTION_WIKI );
        }
        
        // Setup controller profiles settings based on ROM's number of players
        if( mRomDetail.players == 1 )
        {
            // Simplify name of "controller 1" to just "controller" to eliminate confusion
            findPreference( CONTROLLER_PROFILE1 ).setTitle( R.string.controllerProfile_title );
            
            // Remove unneeded preference items
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE2 );
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE3 );
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE4 );
            PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, PLAYER_MAP );
        }
        else
        {
            // Remove unneeded preference items
            if( mRomDetail.players < 4 )
                PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE4 );
            if( mRomDetail.players < 3 )
                PrefUtil.removePreference( this, CATEGORY_GAME_SETTINGS, CONTROLLER_PROFILE3 );
            
            // Configure the player map preference
            PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( PLAYER_MAP );
            playerPref.setMogaController( mMogaController );
        }
        
        // Add a new section explaining the region and dump information for the ROM
        // http://forums.emulator-zone.com/archive/index.php/t-5533.html
        List<Preference> prefs = new ArrayList<Preference>();
        
        // There's probably some clever regex to do this, but use basic string functions to parse out the dump info
        int index = 0, length = mRomDetail.goodName.length();
        
        while ( index < length )
        {
            int startIndex = length, endIndex = length;
            int paren = mRomDetail.goodName.indexOf( "(", index );
            int bracket = mRomDetail.goodName.indexOf( "[", index );
            if ( paren > -1 && paren < startIndex ) startIndex = paren + 1;
            if ( bracket > -1 && bracket < startIndex ) startIndex = bracket + 1;
            if ( startIndex >= length ) break;
            
            paren = mRomDetail.goodName.indexOf( ")", startIndex );
            bracket = mRomDetail.goodName.indexOf( "]", startIndex );
            if ( paren > -1 && paren < endIndex ) endIndex = paren;
            if ( bracket > -1 && bracket < endIndex ) endIndex = bracket;
            if ( endIndex >= length ) break;
            
            // parse out the tokens between startIndex and endIndex
            String code = mRomDetail.goodName.substring( startIndex, endIndex );
            
            Preference info = new Preference( this );
            
            if ( code.length() <= 2 )
            {
                if ( code.startsWith( "a" ) )
                {
                    // a# = alternate
                    info.setTitle( getString( R.string.infoAlternate_title ) );
                    info.setSummary( getString( R.string.infoAlternate_summary ) );
                }
                else if ( code.startsWith( "b" ) )
                {
                    // b# = bad dump
                    info.setTitle( getString( R.string.infoBad_title ) );
                    info.setSummary( getString( R.string.infoBad_summary ) );
                }
                else if ( code.startsWith( "t" ) )
                {
                    // t# = trained
                    info.setTitle( getString( R.string.infoTrained_title ) );
                    info.setSummary( getString( R.string.infoTrained_summary ) );
                }
                else if ( code.startsWith( "f" ) )
                {
                    // f# = fix
                    info.setTitle( getString( R.string.infoFixed_title ) );
                    info.setSummary( getString( R.string.infoFixed_summary ) );
                }
                else if ( code.startsWith( "h" ) )
                {
                    // h# = hack
                    info.setTitle( getString( R.string.infoHack_title ) );
                    info.setSummary( getString( R.string.infoHack_summary ) );
                }
                else if ( code.startsWith( "o" ) )
                {
                    // o# = overdump
                    info.setTitle( getString( R.string.infoOverdump_title ) );
                    info.setSummary( getString( R.string.infoOverdump_summary ) );
                }
                else if ( code.equals( "!" ) )
                {
                    // ! = good dump
                    info.setTitle( getString( R.string.infoVerified_title ) );
                    info.setSummary( getString( R.string.infoVerified_summary ) );
                }
                else if ( code.equals( "A" ) )
                {
                    // A = Australia
                    info.setTitle( getString( R.string.infoAustralia_title ) );
                    info.setSummary( getString( R.string.infoAustralia_summary ) );
                }
                else if ( code.equals( "U" ) )
                {
                    // U = USA
                    info.setTitle( getString( R.string.infoUSA_title ) );
                    info.setSummary( getString( R.string.infoUSA_summary ) );
                }
                else if ( code.equals( "J" ) )
                {
                    // J = Japan
                    info.setTitle( getString( R.string.infoJapan_title ) );
                    info.setSummary( getString( R.string.infoJapan_summary ) );
                }
                else if ( code.equals( "JU" ) )
                {
                    // JU = Japan and USA
                    info.setTitle( getString( R.string.infoJapanUSA_title ) );
                    info.setSummary( getString( R.string.infoJapanUSA_summary ) );
                }
                else if ( code.equals( "E" ) )
                {
                    // E = Europe
                    info.setTitle( getString( R.string.infoEurope_title ) );
                    info.setSummary( getString( R.string.infoEurope_summary ) );
                }
                else if ( code.equals( "G" ) )
                {
                    // G = Germany
                    info.setTitle( getString( R.string.infoGermany_title ) );
                    info.setSummary( getString( R.string.infoGermany_summary ) );
                }
                else if ( code.equals( "F" ) )
                {
                    // F = France
                    info.setTitle( getString( R.string.infoFrance_title ) );
                    info.setSummary( getString( R.string.infoFrance_summary ) );
                }
                else if ( code.equals( "S" ) )
                {
                    // S = Spain
                    info.setTitle( getString( R.string.infoSpain_title ) );
                    info.setSummary( getString( R.string.infoSpain_summary ) );
                }
                else if ( code.equals( "I" ) )
                {
                    // I = Italy
                    info.setTitle( getString( R.string.infoItaly_title ) );
                    info.setSummary( getString( R.string.infoItaly_summary ) );
                }
                else if ( code.equals( "PD" ) )
                {
                    // PD = public domain
                    info.setTitle( getString( R.string.infoPublicDomain_title ) );
                    info.setSummary( getString( R.string.infoPublicDomain_summary ) );
                }
                else if ( code.startsWith( "M" ) )
                {
                    // M# = multi-language
                    info.setTitle( getString( R.string.infoLanguage_title, code.substring( 1 ) ) );
                    info.setSummary( getString( R.string.infoLanguage_summary ) );
                }
                else
                {
                    // ignore this code
                    info = null;
                }
            }
            else if ( code.startsWith( "T+" ) )
            {
                // T+* = translated
                info.setTitle( getString( R.string.infoTranslated_title ) );
                info.setSummary( getString( R.string.infoTranslated_summary ) );
            }
            else if ( code.startsWith( "T-" ) )
            {
                // T-* = translated
                info.setTitle( getString( R.string.infoTranslated_title ) );
                info.setSummary( getString( R.string.infoTranslated_summary ) );
            }
            else if ( code.startsWith( "V" ) && code.length() <= 6 )
            {
                // V = version code
                info.setTitle( getString( R.string.infoVersion_title, code.substring(1) ) );
                info.setSummary( getString( R.string.infoVersion_summary ) );
            }
            else if ( code.startsWith( "PAL" ) )
            {
                // PAL = PAL version
                info.setTitle( getString( R.string.infoPAL_title ) );
                info.setSummary( getString( R.string.infoPAL_summary ) );
            }
            else if ( code.startsWith( "PAL-NTSC" ) )
            {
                // PAL-NTSC = PAL and NTSC compatible
                info.setTitle( getString( R.string.infoPALNTSC_title ) );
                info.setSummary( getString( R.string.infoPALNTSC_summary ) );
            }
            else if ( code.startsWith( "NTSC" ) )
            {
                // NTSC = NTSC version
                info.setTitle( getString( R.string.infoNTSC_title ) );
                info.setSummary( getString( R.string.infoNTSC_summary ) );
            }
            else
            {
                // Everything else is listed raw and treated as a hack
                info.setTitle( code );
                info.setSummary( getString( R.string.infoHack_summary ) );
            }
            
            if ( info != null )
                prefs.add( info );
            
            index = endIndex + 1;
        }
        
        if ( prefs.size() > 0 )
        {
            PreferenceCategory infoCategory = new PreferenceCategory( this );
            infoCategory.setTitle( getString( R.string.categoryGameInfo_title ) );
            getPreferenceScreen().addPreference( infoCategory ); 
            
            for ( Preference pref : prefs )
            {
                infoCategory.addPreference( pref );
            }
        }
        
        // Build the cheats category as needed
        refreshCheatsCategory();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
        mMogaController.onResume();
        refreshViews();
        
        if ( ACTION_RESUME.equals( action ) )
        {
            action = ACTION_EXIT;
            launchGame( false );
        }
        else if ( ACTION_RESTART.equals( action ) )
        {
            action = ACTION_EXIT;
            launchGame( true );
        }
        else if ( ACTION_EXIT.equals( action ) )
        {
            action = null;
            finish();
        }
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        mMogaController.onPause();
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMogaController.exit();
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        refreshViews();
        if( key.equals( PLAY_SHOW_CHEATS ) )
        {
            refreshCheatsCategory();
        }
    }
    
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == 111 )
            refreshCheatsCategory();
    }
    
    private void refreshViews()
    {
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        
        // Refresh the preferences objects
        mUserPrefs = new UserPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomHeader );
        
        // Populate the profile preferences
        mEmulationProfile.populateProfiles( mAppData.emulationProfiles_cfg,
                mUserPrefs.emulationProfiles_cfg, mUserPrefs.getEmulationProfileDefault() );
        mTouchscreenProfile.populateProfiles( mAppData.touchscreenProfiles_cfg,
                mUserPrefs.touchscreenProfiles_cfg, mUserPrefs.getTouchscreenProfileDefault() );
        mControllerProfile1.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, mUserPrefs.getControllerProfileDefault() );
        mControllerProfile2.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, "" );
        mControllerProfile3.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, "" );
        mControllerProfile4.populateProfiles( mAppData.controllerProfiles_cfg,
                mUserPrefs.controllerProfiles_cfg, "" );
        
        // Refresh the preferences objects in case populate* changed a value
        mUserPrefs = new UserPrefs( this );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomHeader );
        
        // Set cheats screen summary text
        mScreenCheats.setSummary( mGamePrefs.isCheatOptionsShown
                ? R.string.screenCheats_summaryEnabled
                : R.string.screenCheats_summaryDisabled );
        
        // Enable/disable player map item as necessary
        PrefUtil.enablePreference( this, PLAYER_MAP, mGamePrefs.playerMap.isEnabled() );
        
        // Define which buttons to show in player map dialog
        @SuppressWarnings( "deprecation" )
        PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( PLAYER_MAP );
        if( playerPref != null )
        {
            // Check null in case preference has been removed
            boolean enable1 = mGamePrefs.isControllerEnabled1;
            boolean enable2 = mGamePrefs.isControllerEnabled2 && mRomDetail.players > 1;
            boolean enable3 = mGamePrefs.isControllerEnabled3 && mRomDetail.players > 2;
            boolean enable4 = mGamePrefs.isControllerEnabled4 && mRomDetail.players > 3;
            playerPref.setControllersEnabled( enable1, enable2, enable3, enable4 );
        }
        
        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }
    
    private void refreshCheatsCategory()
    {
        if( mGamePrefs.isCheatOptionsShown )
        {
            // Populate menu items
            buildCheatsCategory( mRomHeader.crc );
            
            // Show the cheats category
            mScreenCheats.addPreference( mCategoryCheats );
        }
        else
        {
            // Hide the cheats category
            mScreenCheats.removePreference( mCategoryCheats );
        }
    }
    
    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        String key = preference.getKey();
        if( key.equals( ACTION_RESUME ) )
        {
            launchGame( false );
            return true;
        }
        else if( key.equals( ACTION_RESTART ) )
        {
            CharSequence title = getText( R.string.confirm_title );
            CharSequence message = getText( R.string.confirmResetGame_message );
            Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
            {
                @Override
                public void onConfirm()
                {
                    launchGame( true );
                }
            } );
            return true;
        }
        else if( key.equals( ACTION_CHEAT_EDITOR ) )
        {
            Intent intent = new Intent( this, CheatEditorActivity.class );
            intent.putExtra( Keys.Extras.ROM_PATH, mRomPath );
            startActivityForResult( intent, 111 );
        }
        else if( key.equals( ACTION_WIKI ) )
        {
            Utility.launchUri( this, mRomDetail.wikiUrl );
        }
        else if( key.equals( ACTION_RESET_GAME_PREFS ) )
        {
            actionResetGamePrefs();
        }
        else if( key.equals( ACTION_GLOBAL_SETTINGS ) )
        {
            Intent intent = new Intent( this, SettingsGlobalActivity.class );
            intent.putExtra( Keys.Extras.MENU_DISPLAY_MODE, 2 );
            startActivity( intent );
        }
        return false;
    }
    
    private void buildCheatsCategory( final String crc )
    {
        mCategoryCheats.removeAll();
        
        Log.v( "PlayMenuActivity", "building from CRC = " + crc );
        if( crc == null )
            return;
        
        // Get the appropriate section of the config file, using CRC as the key
        CheatFile mupencheat_txt = new CheatFile( mAppData.mupencheat_txt );
        CheatSection cheatSection = mupencheat_txt.match( "^" + crc.replace( ' ', '-' ) + ".*" );
        if( cheatSection == null )
        {
            Log.w( "PlayMenuActivity", "No cheat section found for '" + crc + "'" );
            return;
        }
        ArrayList<Cheat> cheats = new ArrayList<Cheat>();
        cheats.addAll( CheatUtils.populate( crc, mupencheat_txt, true, this ) );
        CheatUtils.reset();
        
        // Layout the menu, populating it with appropriate cheat options
        for( int i = 0; i < cheats.size(); i++ )
        {
            // Get the short title of the cheat (shown in the menu)
            String title;
            if( cheats.get( i ).name == null )
            {
                // Title not available, just use a default string for the menu
                title = getString( R.string.cheats_defaultName, i );
            }
            else
            {
                // Title available, remove the leading/trailing quotation marks
                title = cheats.get( i ).name;
            }
            String notes = cheats.get( i ).desc;
            String options = cheats.get( i ).option;
            String[] optionStrings = null;
            if( !TextUtils.isEmpty( options ) )
            {
                optionStrings = options.split( "\n" );
            }
            
            // Create the menu item associated with this cheat
            CheatPreference pref = new CheatPreference( this, title, notes, optionStrings );
            pref.setKey( crc + " Cheat" + i );
            
            // Add the preference menu item to the cheats category
            mCategoryCheats.addPreference( pref );
        }
    }
    
    private void launchGame( boolean isRestarting )
    {
        // Popup the multi-player dialog if necessary and abort if any players are unassigned
        if( mRomDetail.players > 1 && mGamePrefs.playerMap.isEnabled()
                && mUserPrefs.getPlayerMapReminder() )
        {
            mGamePrefs.playerMap.removeUnavailableMappings();
            boolean needs1 = mGamePrefs.isControllerEnabled1 && !mGamePrefs.playerMap.isMapped( 1 );
            boolean needs2 = mGamePrefs.isControllerEnabled2 && !mGamePrefs.playerMap.isMapped( 2 );
            boolean needs3 = mGamePrefs.isControllerEnabled3 && !mGamePrefs.playerMap.isMapped( 3 )
                    && mRomDetail.players > 2;
            boolean needs4 = mGamePrefs.isControllerEnabled4 && !mGamePrefs.playerMap.isMapped( 4 )
                    && mRomDetail.players > 3;
            
            if( needs1 || needs2 || needs3 || needs4 )
            {
                @SuppressWarnings( "deprecation" )
                PlayerMapPreference pref = (PlayerMapPreference) findPreference( "playerMap" );
                pref.show();
                return;
            }
        }
        
        // Make sure that the storage is accessible
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "CheatMenuHandler", "SD Card not accessible in method onPreferenceClick" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );
            return;
        }
        
        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_launchingEmulator );
        
        // Update the ConfigSection with the new value for lastPlayed
        String lastPlayed = Integer.toString( (int) ( new Date().getTime()/1000 ) );
        ConfigFile config = new ConfigFile( mUserPrefs.romInfoCache_cfg );
        if ( config != null )
        {
            config.put( mRomMd5, "lastPlayed", lastPlayed );
            config.save();
        }
        
        // Launch the appropriate game activity
        Intent intent = mUserPrefs.isTouchpadEnabled ? new Intent( this,
                GameActivityXperiaPlay.class ) : new Intent( this, GameActivity.class );
        
        // Pass the startup info via the intent
        intent.putExtra( Keys.Extras.ROM_PATH, mRomPath );
        intent.putExtra( Keys.Extras.ROM_MD5, mRomMd5 );
        intent.putExtra( Keys.Extras.CHEAT_ARGS, getCheatArgs() );
        intent.putExtra( Keys.Extras.DO_RESTART, isRestarting );
        
        startActivity( intent );
    }
    
    @SuppressWarnings( "deprecation" )
    private String getCheatArgs()
    {
        String cheatArgs = null;
        
        PreferenceCategory cheatsCategory = (PreferenceCategory) findPreference( CATEGORY_CHEATS );
        if( cheatsCategory != null )
        {
            for( int i = 0; i < cheatsCategory.getPreferenceCount(); i++ )
            {
                CheatPreference pref = (CheatPreference) cheatsCategory.getPreference( i );
                if( pref.isCheatEnabled() )
                {
                    if( cheatArgs == null )
                        cheatArgs = ""; // First time through
                    else
                        cheatArgs += ",";
                    
                    cheatArgs += pref.getCheatCodeString( i );
                }
            }
        }
        
        return cheatArgs;
    }
    
    private void actionResetGamePrefs()
    {
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.actionResetGamePrefs_popupMessage );
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                // Reset the user preferences
                mPrefs.unregisterOnSharedPreferenceChangeListener( PlayMenuActivity.this );
                mPrefs.edit().clear().commit();
                PreferenceManager.setDefaultValues( PlayMenuActivity.this, R.xml.preferences_game, true );
                
                // Also reset any manual overrides the user may have made in the config file
                File configFile = new File( mGamePrefs.mupen64plus_cfg );
                if( configFile.exists() )
                    configFile.delete();
                
                // Rebuild the menu system by restarting the activity
                finish();
                startActivity( getIntent() );
            }
        } );
    }
}
