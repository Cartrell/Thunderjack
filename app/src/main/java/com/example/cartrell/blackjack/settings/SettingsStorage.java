package com.example.cartrell.blackjack.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.cartrell.blackjack.MainActivity;

public class SettingsStorage {
  //=========================================================================
  // members
  //=========================================================================
  private Activity m_activity;

  //=========================================================================
  // public
  //=========================================================================

  //-------------------------------------------------------------------------
  // ctor
  //-------------------------------------------------------------------------
  public SettingsStorage(Activity activity) {
    //m_activity = activity;
  }

  //-------------------------------------------------------------------------
  // read
  //-------------------------------------------------------------------------
  public void read(Settings targetSettings) {
    SharedPreferences sharedPreferences = MainActivity.sm_mainActivity.getPreferences(Context.MODE_PRIVATE);
    targetSettings.setCredits(sharedPreferences.getInt("credits", 0));
    targetSettings.setIsSoundOn(sharedPreferences.getBoolean("isSoundOn", true));
    targetSettings.setIsMusicOn(sharedPreferences.getBoolean("isMusicOn", true));
    targetSettings.setTotalGamesPlayed(sharedPreferences.getInt("totalGamesPlayed", 0));
    targetSettings.setTotalGamesWon(sharedPreferences.getInt("totalGamesWon", 0));
    targetSettings.setNumBj(sharedPreferences.getInt("numBj", 0));
    targetSettings.setNumSplits(sharedPreferences.getInt("numSplits", 0));
    targetSettings.setNumSurrenders(sharedPreferences.getInt("numSurrenders", 0));
    targetSettings.setNumBlitzs(sharedPreferences.getInt("numBlitzs", 0));
  }

  //-------------------------------------------------------------------------
  // write
  //-------------------------------------------------------------------------
  public void write(Settings sourceSettings) {
    SharedPreferences sharedPreferences = MainActivity.sm_mainActivity.getPreferences(Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putInt("credits", sourceSettings.getCredits());
    editor.putBoolean("isSoundOn", sourceSettings.getIsSoundOn());
    editor.putBoolean("isMusicOn", sourceSettings.getIsMusicOn());
    editor.putInt("totalGamesPlayed", sourceSettings.getTotalGamesPlayed());
    editor.putInt("totalGamesWon", sourceSettings.getTotalGamesWon());
    editor.putInt("numBj", sourceSettings.getNumBj());
    editor.putInt("numSplits", sourceSettings.getNumSplits());
    editor.putInt("numSurrenders", sourceSettings.getNumSurrenders());
    editor.putInt("numBlitzs", sourceSettings.getNumBlitzs());
    editor.apply();
  }

  //=========================================================================
  // private
  //=========================================================================
}