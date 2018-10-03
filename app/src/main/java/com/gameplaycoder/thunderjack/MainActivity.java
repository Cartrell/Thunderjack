package com.gameplaycoder.thunderjack;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gameplaycoder.thunderjack.databinding.ActivityMainBinding;
import com.gameplaycoder.thunderjack.engine.BjEngine;
import com.gameplaycoder.thunderjack.utils.CreditsRenewalChecker;

public class MainActivity extends AppCompatActivity {
  //=========================================================================
  // static / const
  //=========================================================================
  public static MainActivity sm_mainActivity;

  //=========================================================================
  // members
  //=========================================================================
  private ActivityMainBinding m_binding;
  private BjEngine m_engine;

  //=========================================================================
  // public
  //=========================================================================

  //=========================================================================
  // protected
  //=========================================================================

  //-------------------------------------------------------------------------
  // initEngine
  //-------------------------------------------------------------------------
  private void initEngine() {
    m_engine = new BjEngine(this, m_binding);
  }

  //-------------------------------------------------------------------------
  // onCreate
  //-------------------------------------------------------------------------
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    m_binding = DataBindingUtil.setContentView(this,  R.layout.activity_main);
    sm_mainActivity = this;
    initEngine();
  }

  //-------------------------------------------------------------------------
  // onStop
  //-------------------------------------------------------------------------
  @Override
  protected void onStop() {
    super.onStop();
    updateLastGameClosedTimeWithLowCredits();
  }

  //=========================================================================
  // private
  //=========================================================================

  //-------------------------------------------------------------------------
  // updateLastGameClosedTimeWithLowCredits
  //-------------------------------------------------------------------------
  private void updateLastGameClosedTimeWithLowCredits() {
    if (m_engine == null) {
      return;
    }

    CreditsRenewalChecker creditsRenewalChecker = new CreditsRenewalChecker(m_engine);
    int credits = m_engine.getCredits();
    if (creditsRenewalChecker.shouldTimeBeUpdated(credits)) {
      m_engine.updateLastGameClosedTimeWithLowCredits();
    }
  }
}