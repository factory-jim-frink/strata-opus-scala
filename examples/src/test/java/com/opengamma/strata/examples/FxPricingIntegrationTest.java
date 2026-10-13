/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import static com.opengamma.strata.collect.TestHelper.caputureSystemOut;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Integration test for the FX pricing example.
 */
public class FxPricingIntegrationTest {

  @Test
  public void test_fxPricing_endToEnd() {
    String report = caputureSystemOut(() -> FxPricingExample.main(new String[0]));

    assertThat(report)
        .contains("+------", "GBP 10,000/USD @ 1.62 fwd", "GBP 10,000/USD @ 1.62 swap",
            "Bullet payment GBP 20,000", "NPV", "PV01");
    assertThat(report).doesNotContain("ERROR", "FAIL", "Exception", "drill down");
  }

}
