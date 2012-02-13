/*
 * eID Middleware Project.
 * Copyright (C) 2010-2012 FedICT.
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

package be.fedict.eidviewer.lib.file.helper;

import be.fedict.eid.applet.service.Gender;
import be.fedict.eid.applet.service.Identity;
import be.fedict.eid.applet.service.SpecialStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 *
 * @author Frank Marien
 */
public class TextFormatHelper
{
    public static final String UNKNOWN_VALUE_TEXT = "-";
    private static final Logger logger = Logger.getLogger(TextFormatHelper.class.getName());
    
    // return whatever text is available for gender in bundle
    public static String getGenderString(ResourceBundle bundle, Gender gender)
    {
        return gender==Gender.FEMALE ? bundle.getString("genderFemale") : bundle.getString("genderMale");
    }

    // return special stati, comma separated
    public static String getSpecialStatusString(ResourceBundle bundle, SpecialStatus specialStatus)
    {
        List<String> specials = new ArrayList<String>();

        if(specialStatus!=null)
        {
            if (specialStatus.hasWhiteCane())
                specials.add(bundle.getString("special_status_white_cane"));
            if (specialStatus.hasYellowCane())
                specials.add(bundle.getString("special_status_yellow_cane"));
            if (specialStatus.hasExtendedMinority())
                specials.add(bundle.getString("special_status_extended_minority"));
        }
        return TextFormatHelper.join(specials, ",");
    }
    
    // format a national number into YY.MM.DD-S&G.CS
    public static String formatNationalNumber(String nationalNumber)
    {
        //YY MM DD S&G CS
        //01 23 45 678 9A
        
        StringBuilder formatted=new StringBuilder(nationalNumber.substring(0,2));
                      formatted.append('.');
                      formatted.append(nationalNumber.substring(2,4));
                      formatted.append('.');
                      formatted.append(nationalNumber.substring(4,6));
                      formatted.append('-');
                      formatted.append(nationalNumber.substring(6,9));
                      formatted.append('.');
                      formatted.append(nationalNumber.substring(9));
        return formatted.toString();
    }
    
    /*
     *  format a card number into XXX-YYYYYYYY-ZZ
     */
    public static String formatCardNumber(String cardNumber)
    {
        //XXX-XXXXXXX-XX
        //012 3456789 AB
        
        StringBuilder formatted=new StringBuilder(cardNumber.substring(0,3));
                      formatted.append('-');
                      formatted.append(cardNumber.substring(3,10));
                      formatted.append('-');
                      formatted.append(cardNumber.substring(10));
                     
        return formatted.toString();
    }
    
    public static String createFullNameString(String firstName, String givenNames, String familyName)
    {
        StringBuilder builder=new StringBuilder(firstName);
        
        if(givenNames!=null)
        {
            builder.append(' ');
            builder.append(givenNames);
        }
        
        if(familyName!=null)
        {
            builder.append(' ');
            builder.append(familyName);
        }
        
        return builder.toString();
    }
    
    public static int setFirstNamesFromStrings(Identity identity, String givenNames, String firstLetterOf3rdGivenName)
    {
        StringBuilder builder=new StringBuilder(givenNames);
        
        if(firstLetterOf3rdGivenName!=null)
        {
            builder.append(' ');
            builder.append(firstLetterOf3rdGivenName);
        }
        
        return setFirstNamesFromString(identity,builder.toString());
    }
    
    public static int setFirstNamesFromString(Identity identity, String names)
    {
        String[] nameParts = names.split(" ");
        if(nameParts.length==1)
        {
            logger.finest("First Name: One Token -> firstname, no middleName");
            identity.firstName = nameParts[0];  
        }
        else
        {
            logger.finest("First Name: Two Or More Tokens -> one in firstname, others in middleName");
            identity.firstName = nameParts[0];

            StringBuilder middleNames = new StringBuilder();
            for (int i = 1; i <= nameParts.length - 1; i++)
            {
                middleNames.append(nameParts[i]);
                middleNames.append(' ');
            }

            identity.middleName = middleNames.toString().trim();
        }
        
        return nameParts.length;
    }
    
    // join a la python etc..
    public static String join(Collection<?> s, String delimiter)
    {
        StringBuilder buffer = new StringBuilder();
        Iterator<?> iter = s.iterator();
        if (iter.hasNext())
        {
            buffer.append(iter.next());
            while (iter.hasNext())
            {
                buffer.append(delimiter);
                buffer.append(iter.next());
            }
        }
        return buffer.toString();
    }
}
