/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.fedict.eidviewer.gui.helper;

import java.util.Locale;

/**
 *
 * @author Frank Marien
 */
public class ComparableLocale implements Comparable
{
    private Locale locale;
    
    public ComparableLocale(String language)
    {
        locale=new Locale(language);
    }

    public ComparableLocale(String language, String country)
    {
        locale=new Locale(language, country);
    }

    public ComparableLocale(String language, String country, String variant)
    {
        locale=new Locale(language, country, variant);
    }
    
    @Override
    public String toString()
    {
        return locale.getDisplayLanguage();
    }
    
    public Locale getLocale()
    {
        return this.locale;
    }

    public int compareTo(Object thatObject)
    {
        if(!(thatObject instanceof ComparableLocale))
            throw new ClassCastException("Cannot Compare " + thatObject.getClass().getName() + " to " + this.getClass().getName());
        ComparableLocale that=(ComparableLocale)thatObject;
        Locale thisLocale=this.getLocale();
        Locale thatLocale=that.getLocale();
        String thisStr=thisLocale.getLanguage() + "_" + thisLocale.getCountry() + "_" + thisLocale.getVariant();
        String thatStr=thatLocale.getLanguage() + "_" + thatLocale.getCountry() + "_" + thatLocale.getVariant();
        return thisStr.compareTo(thatStr);
    }

    @Override
    public boolean equals(Object thatObject)
    {
        if(!(thatObject instanceof ComparableLocale))
            throw new ClassCastException("Cannot Compare " + thatObject.getClass().getName() + " to " + this.getClass().getName());
        ComparableLocale that=(ComparableLocale)thatObject;
        return this.compareTo(thatObject)==0;
    }

    @Override
    public int hashCode()
    {
        return this.locale.hashCode();
    }  
    
    public static ComparableLocale fromLocale(Locale locale)
    {
        return new ComparableLocale(locale.getLanguage(),locale.getCountry());
    }
}
