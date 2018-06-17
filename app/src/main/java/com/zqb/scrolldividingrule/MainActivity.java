package com.zqb.scrolldividingrule;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

  private boolean isDividingScroll;
  private EditText editText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final ScrollDividingRuleView scrollDividingRuleView = findViewById(R.id.scroll_dividing_rule_view);
    editText = findViewById(R.id.edit_text);
    scrollDividingRuleView.bindMoneyData(0, 100000, 100,
        new ScrollDividingRuleView.OnScrollListener() {
          @Override public void onScaleScrollChanged(long scale) {
            isDividingScroll=true;//防止滚动的时候和edittext绑定数据出问题
            editText.setText(String.valueOf(scale*100));
            editText.setSelection(String.valueOf(scale*100).length());
          }
        });

    scrollDividingRuleView.setScaleMargin(30)
        .setTextLineMargin(30);

    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        String money = s.toString().trim();
        if (money.length() > 0 && !TextUtils.isEmpty(money)) {
          float i = Float.parseFloat(money);
            if (!isDividingScroll) {
              scrollDividingRuleView.setNowScale(i >= 100? i / 100 : 0);
            } else {
              isDividingScroll = false;
            }
          }
      }
    });
  }
}
