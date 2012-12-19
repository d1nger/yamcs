package org.yamcs;


import com.google.common.util.concurrent.Service;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.xtce.Parameter;
/**
 * interface implemented by all the classes that can provide parameters to the ParameterRequestManager
 * @author mache
 *
 */
public interface ParameterProvider extends Service {
    /**
     * 
     * @param parameterRequestManager
     */
    public abstract void setParameterListener(ParameterListener parameterRequestManager);
	/**
	 * Adds a new parameter to the list  of parameters that have to provided 
	 * @param paramDef
	 */
	public abstract void startProviding(Parameter paramDef);
	
	/**
	 * start providing all known parameters
	 */
	public abstract void startProvidingAll();
	
	/**
	 * Removes a parameter from the list of parameters that have to be provided
	 * @param paramDef
	 */
	public abstract void stopProviding(Parameter paramDef);
	/**
	 * Returns whether or not a given parameter can be provided by this provider
	 * @param itemId
	 * @return
	 */
	public abstract boolean canProvide(NamedObjectId para);
	/**
	 * Returns the parameterDefinition corresponding to the itemId
	 * @param itemId
	 * @return
	 * @throws InvalidIdentification
	 */
	public abstract Parameter getParameter(NamedObjectId para) throws InvalidIdentification;
    public abstract String getDetailedStatus();
}