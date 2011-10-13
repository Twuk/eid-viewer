/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.fedict.eidviewer.gui.helper;

import be.fedict.eid.applet.service.Identity;
import java.util.logging.Logger;

/**
 *
 * @author frank
 */
public class MiddleNamesHelper
{
    private static final Logger logger = Logger.getLogger(MiddleNamesHelper.class.getName());

    public static int setFirstNamesFromString(Identity identity, String names)
    {
        String[] nameParts = names.split(" ");
        switch (nameParts.length)
        {
            case 1:
                logger.finest("First Name: One Token -> firstname, no middleName");
                identity.firstName = nameParts[0];
                break;

            case 2:
                logger.finest("First Name: Two Tokens -> one in firstName, second in middleName");
                identity.firstName = nameParts[0];
                identity.middleName = nameParts[1];
                break;

            default:
            {
                logger.finest("First Name: More Than Two Tokens -> all but last part in firstname, last part in middleName");
                StringBuilder firstName = new StringBuilder();
                for (int i = 0; i <= nameParts.length - 2; i++)
                {
                    firstName.append(nameParts[i]);
                    firstName.append(' ');
                }

                identity.firstName = firstName.toString().trim();
                identity.middleName = nameParts[nameParts.length - 1];
            }
            break;
        }
        
        return nameParts.length;
    }
}
