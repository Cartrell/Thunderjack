package com.example.cartrell.blackjack.players;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.support.constraint.Guideline;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.cartrell.blackjack.engine.BjBetChip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class HandData extends BaseHandData {
  //=========================================================================
  // const
  //=========================================================================
  private final int MAX_CHIPS = 4;

  //=========================================================================
  // members
  //=========================================================================
  private ArrayList<BjBetChip> m_betChips;
  private ViewDistributor m_betChipsViewDistributor;
  private ImageView m_turnIndicatorImage;
  private TextView m_betValueAndAmountWonText;
  private Point m_chipSize;
  private float m_xChipsLeft;
  private float m_xChipsRight;
  private float m_yChipsTop;

  //=========================================================================
  // package-private
  //=========================================================================

  //-------------------------------------------------------------------------
  // ctor
  //-------------------------------------------------------------------------
  HandData(ViewGroup viewGroup, float xDeck, float yDeck, int maxCardsPerHand,
  Guideline guideCardsLeft, Guideline guideCardsRight, Guideline guideCardsTop,
  Guideline guideCardsBottom, Guideline guideCardsUi, int cardImageWidth, TextView scoreText,
  ImageView resultImage, HashMap<String, Object> extraParams) {
    super(viewGroup, xDeck, yDeck, maxCardsPerHand, guideCardsLeft, guideCardsRight,
      guideCardsTop, guideCardsBottom, guideCardsUi, cardImageWidth, scoreText, resultImage,
      extraParams);
    m_betChips = new ArrayList<>();
    initHdComponents(extraParams);
  }

  //-------------------------------------------------------------------------
  // addBetChip
  //-------------------------------------------------------------------------
  void addBetChip(BjBetChip betChip) {
    m_betChips.add(betChip);

    ImageView betChipImage = betChip.getImage();

    //Point size = Metrics.CalcSize(betChipImage, m_yCardsBottom, m_yChipsTop, false);
    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(m_chipSize.x, m_chipSize.y);
    m_viewGroup.addView(betChipImage, params);

    if (m_betChipsViewDistributor == null) {
      m_betChipsViewDistributor = new ViewDistributor(m_xChipsLeft,
        m_xChipsRight - m_chipSize.x, m_yChipsTop, MAX_CHIPS, m_chipSize.x);
    }

    m_betChipsViewDistributor.add(betChipImage);
    updateBetChipPositions();
  }

  //-------------------------------------------------------------------------
  // hideBetChips
  //-------------------------------------------------------------------------
  void hideBetChips() {
    for (BjBetChip betChip : m_betChips) {
      betChip.getImage().setVisibility(View.INVISIBLE);
    }
  }

  //-------------------------------------------------------------------------
  // removeBetChips
  //-------------------------------------------------------------------------
  void removeBetChips() {
    for (BjBetChip betChip : m_betChips) {
      m_viewGroup.removeView(betChip.getImage());
    }

    m_betChips.clear();

    if (m_betChipsViewDistributor != null) {
      m_betChipsViewDistributor.removeAll();
    }
  }

  //-------------------------------------------------------------------------
  // setBetAmountWonValue
  //-------------------------------------------------------------------------
  void setBetAmountWonValue(int value) {
    setText(m_betValueAndAmountWonText, value);
  }

  //-------------------------------------------------------------------------
  // setBetAmountWonValueVisible
  //-------------------------------------------------------------------------
  void setBetAmountWonValueVisible(boolean isVisible) {
    showView(m_betValueAndAmountWonText, isVisible);
  }

  //-------------------------------------------------------------------------
  // showBetChips
  //-------------------------------------------------------------------------
  void showBetChips() {
    for (BjBetChip betChip : m_betChips) {
      betChip.getImage().setVisibility(View.VISIBLE);
    }
  }

  //-------------------------------------------------------------------------
  // showTurnIndicatorImage
  //-------------------------------------------------------------------------
  void showTurnIndicatorImage(boolean isVisible) {
    showView(m_turnIndicatorImage, isVisible);
    animateDrawable(m_turnIndicatorImage, isVisible);
  }

  //=========================================================================
  // private
  //=========================================================================

  //-------------------------------------------------------------------------
  // animateDrawable
  //-------------------------------------------------------------------------
  private void animateDrawable(ImageView imageView, boolean doAnimate) {
    AnimationDrawable animation = imageView != null ? (AnimationDrawable)imageView.getBackground() : null;
    if (animation == null) {
      return; //sanity checks
    }

    if (doAnimate) {
      animation.start();
    } else {
      animation.stop();
    }
  }

  //-------------------------------------------------------------------------
  // initHdComponents
  //-------------------------------------------------------------------------
  private void initHdComponents(HashMap<String, Object> extraParams) {
    m_betValueAndAmountWonText = (TextView)extraParams.get("betValueText");
    m_xChipsLeft = ((Guideline)extraParams.get("guideChipsLeft")).getX();
    m_xChipsRight = ((Guideline)extraParams.get("guideChipsRight")).getX();
    m_yChipsTop = ((Guideline)extraParams.get("guideChipsTop")).getY();
    m_chipSize = (Point)extraParams.get("chipSize");
  }

  //-------------------------------------------------------------------------
  // updateBetChipPositions
  //-------------------------------------------------------------------------
  private void updateBetChipPositions() {
    HashMap<View, PointF>positions = m_betChipsViewDistributor.getPositions();

    Iterator iterator = positions.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry pair = (Map.Entry)iterator.next();
      ImageView betChipImage = (ImageView)pair.getKey();
      PointF position = (PointF)pair.getValue();
      betChipImage.setX(position.x);
      betChipImage.setY(position.y);
    }
  }
}