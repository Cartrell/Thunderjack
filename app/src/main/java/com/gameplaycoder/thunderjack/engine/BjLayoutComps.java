package com.gameplaycoder.thunderjack.engine;

import android.content.Context;
import android.graphics.Point;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.widget.ImageView;

import com.gameplaycoder.thunderjack.R;
import com.gameplaycoder.thunderjack.layouts.BaseLayoutComp;
import com.gameplaycoder.thunderjack.layouts.BetAndChipButtons;
import com.gameplaycoder.thunderjack.layouts.BetAndCreditsTexts;
import com.gameplaycoder.thunderjack.layouts.GameBackground;
import com.gameplaycoder.thunderjack.layouts.GameButtons;
import com.gameplaycoder.thunderjack.layouts.LowerResultsImages;
import com.gameplaycoder.thunderjack.layouts.LowerScoreAmountWonTexts;
import com.gameplaycoder.thunderjack.layouts.LowerScoreBetValueTexts;
import com.gameplaycoder.thunderjack.layouts.LowerScoreTexts;
import com.gameplaycoder.thunderjack.layouts.LowerTurnPlayerIndicators;
import com.gameplaycoder.thunderjack.layouts.MidResultsImages;
import com.gameplaycoder.thunderjack.layouts.MidScoreAmountWonTexts;
import com.gameplaycoder.thunderjack.layouts.MidScoreBetValueTexts;
import com.gameplaycoder.thunderjack.layouts.MidScoreTexts;
import com.gameplaycoder.thunderjack.layouts.MidTurnPlayerIndicators;
import com.gameplaycoder.thunderjack.layouts.PlayerBetButtons;
import com.gameplaycoder.thunderjack.layouts.SettingsButtonAndDeck;
import com.gameplaycoder.thunderjack.layouts.UpperResultsImages;
import com.gameplaycoder.thunderjack.layouts.UpperScoreTexts;
import com.gameplaycoder.thunderjack.utils.Metrics;

import java.util.ArrayList;

class BjLayoutComps {
  //=========================================================================
  // members
  //=========================================================================
  GameBackground gameBackground;
  LowerScoreTexts lowerScoreTexts;
  MidScoreTexts midScoreTexts;
  UpperScoreTexts upperScoreTexts;
  LowerScoreBetValueTexts lowerScoreBetValueTexts;
  LowerScoreAmountWonTexts lowerScoreAmountWonTexts;
  MidScoreBetValueTexts midScoreBetValueTexts;
  MidScoreAmountWonTexts midScoreAmountWonTexts;
  LowerResultsImages lowerResultsImages;
  MidResultsImages midResultsImages;
  UpperResultsImages upperResultsImages;
  LowerTurnPlayerIndicators lowerTurnPlayerIndicators;
  MidTurnPlayerIndicators midTurnPlayerIndicators;
  BetAndChipButtons betAndChipButtons;
  GameButtons gameButtons;
  BetAndCreditsTexts betAndCreditsTexts;
  SettingsButtonAndDeck settingsButtonAndDeck;
  PlayerBetButtons playerBetButtons;

  private SparseArray<Guideline> m_guidelines;
  private ConstraintLayout m_containerLayout;
  private LayoutInflater m_inflater;
  private Point m_cardSize;
  private Point m_chipSize;

  //=========================================================================
  // package-private
  //=========================================================================

  //-------------------------------------------------------------------------
  // ctor
  //-------------------------------------------------------------------------
  BjLayoutComps(Context context, ConstraintLayout containerLayout) {
    m_guidelines = new SparseArray<>();

    LayoutInflater inflater = LayoutInflater.from(context);

    m_containerLayout = containerLayout;
    m_inflater = inflater;

    /*
      Order in which layout comps are added matters. Each successive layout comp is added on
      top of the preceding one.

      When the layout comp is inflated (see the private inflate method of this class), the inflated
      view is added manually to the container.

      This is because setting attachToRoot to true causes the inflated view to not be directly
      added to the container layout.
    */
    gameBackground = new GameBackground(inflate(R.layout.game_background));
    addGuidelines(gameBackground);

    settingsButtonAndDeck = new SettingsButtonAndDeck(inflate(R.layout.settings_button_and_deck));
    addGuidelines(settingsButtonAndDeck);

    lowerScoreTexts = new LowerScoreTexts(inflate(R.layout.lower_score_texts));
    addGuidelines(lowerScoreTexts);

    midScoreTexts = new MidScoreTexts(inflate(R.layout.mid_score_texts));
    addGuidelines(midScoreTexts);

    upperScoreTexts = new UpperScoreTexts(inflate(R.layout.upper_score_texts));
    addGuidelines(upperScoreTexts);

    lowerScoreBetValueTexts = new LowerScoreBetValueTexts(inflate(R.layout.lower_score_bet_value_texts));
    addGuidelines(lowerScoreBetValueTexts);

    lowerScoreAmountWonTexts = new LowerScoreAmountWonTexts(inflate(R.layout.lower_score_amount_won_texts));
    addGuidelines(lowerScoreAmountWonTexts);

    midScoreBetValueTexts = new MidScoreBetValueTexts(inflate(R.layout.mid_score_bet_value_texts));
    addGuidelines(midScoreBetValueTexts);

    midScoreAmountWonTexts = new MidScoreAmountWonTexts(inflate(R.layout.mid_score_amount_won_texts));
    addGuidelines(midScoreAmountWonTexts);

    lowerResultsImages = new LowerResultsImages(inflate(R.layout.lower_results_images));
    addGuidelines(lowerResultsImages);

    midResultsImages = new MidResultsImages(inflate(R.layout.mid_results_images));
    addGuidelines(midResultsImages);

    upperResultsImages = new UpperResultsImages(inflate(R.layout.upper_results_images));
    addGuidelines(upperResultsImages);

    lowerTurnPlayerIndicators = new LowerTurnPlayerIndicators(inflate(R.layout.lower_turn_player_indicators));
    addGuidelines(lowerTurnPlayerIndicators);

    midTurnPlayerIndicators = new MidTurnPlayerIndicators(inflate(R.layout.mid_turn_player_indicators));
    addGuidelines(midTurnPlayerIndicators);

    betAndChipButtons = new BetAndChipButtons(inflate(R.layout.bet_and_chip_buttons));
    addGuidelines(betAndChipButtons);

    gameButtons = new GameButtons(inflate(R.layout.game_buttons));
    addGuidelines(gameButtons);

    betAndCreditsTexts = new BetAndCreditsTexts(inflate(R.layout.bet_and_credits_texts));
    addGuidelines(betAndCreditsTexts);

    playerBetButtons = new PlayerBetButtons(inflate(R.layout.player_bet_buttons));
    addGuidelines(playerBetButtons);

    initCardSize();
    initChipSize();

    m_containerLayout = null;
    m_inflater = null;
  }

  //-------------------------------------------------------------------------
  // getCardSize
  //-------------------------------------------------------------------------
  Point getCardSize() {
    return(m_cardSize);
  }

  //-------------------------------------------------------------------------
  // getChipSize
  //-------------------------------------------------------------------------
  Point getChipSize() {
    return(m_chipSize);
  }

  //-------------------------------------------------------------------------
  // getGuideline
  //-------------------------------------------------------------------------
  Guideline getGuideline(int resourceId) {
    return(m_guidelines.get(resourceId));
  }

  //=========================================================================
  // private
  //=========================================================================

  //-------------------------------------------------------------------------
  // addGuidelines
  //-------------------------------------------------------------------------
  private void addGuidelines(BaseLayoutComp layoutComp) {
    ArrayList<Guideline> guidelines = layoutComp.getGuideLines();
    for (Guideline guideline : guidelines) {
      m_guidelines.put(guideline.getId(), guideline);
    }
  }

  //-------------------------------------------------------------------------
  // initCardSize
  //-------------------------------------------------------------------------
  private void initCardSize() {
    Context context = m_inflater.getContext();
    String imageKey = "card_as";

    ImageView image = new ImageView(context);
    image.setImageResource(context.getResources().getIdentifier(imageKey,"drawable",
      context.getPackageName()));
    image.setAdjustViewBounds(true);

    m_cardSize = Metrics.CalcSize(image,
      Metrics.CalcGuidelinePosition(getGuideline(R.id.guideMidCardsBottom)),
      Metrics.CalcGuidelinePosition(getGuideline(R.id.guideMidCardsTop)),
      false);
  }

  //-------------------------------------------------------------------------
  // initChipSize
  //-------------------------------------------------------------------------
  private void initChipSize() {
    Context context = m_inflater.getContext();
    String imageKey = "chip_blue";

    ImageView image = new ImageView(context);
    image.setImageResource(context.getResources().getIdentifier(imageKey,"drawable",
      context.getPackageName()));
    image.setAdjustViewBounds(true);

    m_chipSize = Metrics.CalcSize(image,
      Metrics.CalcGuidelinePosition(getGuideline(R.id.guideMidCardsBottom)),
      Metrics.CalcGuidelinePosition(getGuideline(R.id.guideMidChipsTop)),
      false);
  }

  //-------------------------------------------------------------------------
  // inflate
  //-------------------------------------------------------------------------
  private ConstraintLayout inflate(int resourceLayoutId) {
    ConstraintLayout layout = (ConstraintLayout)m_inflater.inflate(resourceLayoutId,
      m_containerLayout, false);
    m_containerLayout.addView(layout);
    return(layout);
  }
}