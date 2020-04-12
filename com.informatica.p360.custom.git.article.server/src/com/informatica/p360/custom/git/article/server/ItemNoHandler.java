/**************************************************************************************************
*  ____  _  _  ____  _____  ____  __  __    __   ____  ____  ___    __   
* (_  _)( \( )( ___)(  _  )(  _ \(  \/  )  /__\ (_  _)(_  _)/ __)  /__\  
*  _)(_  )  (  )__)  )(_)(  )   / )    (  /(__)\  )(   _)(_( (__  /(__)\ 
* (____)(_)\_)(__)  (_____)(_)\_)(_/\/\_)(__)(__)(__) (____)\___)(__)(__) 
* 
* Informatica MDM
*
* Copyright: Informatica LLC. (c) 2003-2019.  All rights reserved.
* 
*************************************************************************************************/

package com.informatica.p360.custom.git.article.server;

import org.eclipse.core.runtime.CoreException;

import com.heiler.ppm.communication.EasyMapMessage;
import com.heiler.ppm.communication.Message;
import com.heiler.ppm.communication.core.request.RequestContext;
import com.heiler.ppm.communication.core.request.RequestHandler;
import com.heiler.ppm.db.core.DataSources;
import com.heiler.ppm.persistence.server.PersistenceComponent;
import com.heiler.uda.IUDACallableStatement;
import com.heiler.uda.IUDAConnection;
import com.heiler.uda.IUDAValueGetter;
import com.heiler.uda.UDA;
import com.heiler.uda.UDADirection;

/**
 * @author T98711
 */
public class ItemNoHandler implements RequestHandler
{

	@Override
	public Object onMessage(RequestContext requestContext, Message message) throws CoreException, InterruptedException 
	
	
	{
	    String nextItemNumber = "";
	    // TODO Auto-generated method stub
	    EasyMapMessage msg = ( EasyMapMessage ) message;
	    
	    
	    
	   
	  	    IUDAConnection con = null;
	    	    //return getNextItemNo() + StringUtils.EMPTY;

	    	    try
	    	    {
	    	      UDA uda = PersistenceComponent.getUda();

	    	      // get the next number from the stored procedure
	    	      // the procedure will increment it and store it back in the DB
	    	      con = uda.getConnection( DataSources.DB_MAIN );
	    	      IUDACallableStatement cs = con.prepareCall( "CUST_ITEMNMGN_DEFAULT" ); //$NON-NLS-1$
	    	     /* cs.setParameter( "MAXLENGTH", UDADirection.IN ) //$NON-NLS-1$
	    	        .setInt( new Integer( "999999999999999" ) );*/
	    	      cs.setParameter( "ITEMNUM", UDADirection.OUT ) //$NON-NLS-1$
	    	        .setString( nextItemNumber );
	    	      cs.execute();
	    	      IUDAValueGetter parameter = cs.getParameter( "ITEMNUM" );//$NON-NLS-1$
	    	      nextItemNumber = parameter.getString();
	    	      
	    	    }
	    	    catch ( RuntimeException e )
	    	    {
	    	      
	    	      e.printStackTrace();;
	    	    }
	    	    finally
	    	    {

	    	      if ( con != null )
	    	      {
	    	        con.closeSafely();
	    	      }
	    	    }
	    		return nextItemNumber;



	    
	}

}
