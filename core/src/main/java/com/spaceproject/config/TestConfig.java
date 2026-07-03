package com.spaceproject.config;

import java.math.BigDecimal;

public class TestConfig extends Config {
    
    public boolean testA = true;
    public boolean testB = false;
    public boolean testC;
    
    public BigDecimal testD = new BigDecimal(1293458710);
    
    public String testE = "asdf";
    public String testF;
    
    public double testG = -10.123456789;
    public double testH;
    
    public float testI = Float.MIN_VALUE;
    public float testJ = -1234.9876f;
    public float testk;
    
    public int testL = Integer.MAX_VALUE;
    public int testO;
    
    
    @Override
    public void loadDefault() {
    
    }
}
