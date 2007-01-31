/*
 * This file is made available under the terms of the LGPL licence.
 * This licence can be retreived from http://www.gnu.org/copyleft/lesser.html.
 * The source remains the property of the YAWL Foundation.  The YAWL Foundation is a collaboration of
 * individuals and organisations who are commited to improving workflow technology.
 *
 */
package com.nexusbpm.command;

import au.edu.qut.yawl.elements.YSpecification;
import au.edu.qut.yawl.persistence.managed.DataProxy;
import au.edu.qut.yawl.persistence.managed.DataProxyStateChangeListener;

import com.nexusbpm.command.RenameCommandFactory.AbstractRenameCommand;

/**
 * The RenameSpecificationCommand renames a Specification.
 * 
 * @author Nathan Rose
 */
public class RenameSpecificationCommand extends AbstractRenameCommand {
	public RenameSpecificationCommand( DataProxy<YSpecification> proxy, String newName ) {
		super( proxy, newName, proxy.getData().getID() );
	}
    
    protected void rename( String newName, String oldName ) {
        Object data = proxy.getData();
        YSpecification spec = (YSpecification) data;
        
        int lastSlash = spec.getID().lastIndexOf( "/" );
        if( lastSlash != -1 ) {
        	proxy.setLabel( newName);
        }
        else {
        	proxy.setLabel( newName );
        }
        spec.setID( spec.getID().substring(0, lastSlash + 1) + newName);
        proxy.fireUpdated( DataProxyStateChangeListener.PROPERTY_ID, oldName, newName );
    }
}
