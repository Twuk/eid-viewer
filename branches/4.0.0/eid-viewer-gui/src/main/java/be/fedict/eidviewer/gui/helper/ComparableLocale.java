/*
 * eID Middleware Project.
 * Copyright (C) 2010-2011 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see
 * http://www.gnu.org/licenses/.
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
