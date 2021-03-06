/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.converter;

import java.util.Objects;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.data.database.beans.LdapGroup;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.services.ServiceManager;

@Named
public class LdapGroupConverter implements Converter {
    private static final Logger logger = LogManager.getLogger(LdapGroupConverter.class);
    private final ServiceManager serviceManager = new ServiceManager();

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return null;
        } else {
            try {
                return serviceManager.getLdapGroupService().getById(Integer.parseInt(value));
            } catch (DAOException e) {
                logger.error(e.getMessage());
                return "0";
            }
        }
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object value) {
        if (Objects.isNull(value)) {
            return null;
        } else if (value instanceof LdapGroup) {
            return String.valueOf(((LdapGroup) value).getId().intValue());
        } else if (value instanceof String) {
            return (String) value;
        } else {
            throw new ConverterException("Incorrect type: " + value.getClass() + " must be 'LdapGroup'!");
        }
    }
}
