package com.vicious.experiencedworlds.common;

public class EWMath {
    public static double summate(int n, double d1, double dn){
        return n*(d1+dn)/2.0;
    }
    public static double baseToTheX(double base, double x, double yshift){
        if(base < 1) base++;
        return Math.pow(base,x)+yshift;
    }
}
