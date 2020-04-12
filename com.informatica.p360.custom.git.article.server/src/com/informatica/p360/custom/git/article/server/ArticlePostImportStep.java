/*
 * Copyright (c) 2002 Heiler Software AG.
 *
 * Heiler Software and its Licensors own and hold title in and to all
 * intellectual property rights, including but not limited to trademark,
 * service mark, copyright and trade secret rights relating to this Software.
 * This Software and any rights and title to it are protected by national and
 * international Laws and Legislation. Any usage of or changes or modifications
 * to the Software are not permitted without the prior written consent of
 * Heiler Software. Unauthorized Usage of the Software or parts of the Software
 * and any rights to it will be prosecuted by criminal and private law and may
 * result in severe penalties and claims for damages and indemnifications.
 */

package com.informatica.p360.custom.git.article.server;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.sdo.EDataObject;

import com.heiler.ppm.article.core.ArticleCoreConst;
import com.heiler.ppm.catalog.core.entity.CatalogProxy;
import com.heiler.ppm.communication.EasyMapMessage;
import com.heiler.ppm.communication.NodeIdentifier;
import com.heiler.ppm.communication.core.CommunicationException;
import com.heiler.ppm.communication.core.node.TheNode;
import com.heiler.ppm.importer5.core.api.ImportProfile;
import com.heiler.ppm.importer5.core.api.ImportResult;
import com.heiler.ppm.importer5.core.api.ImportResult.ImportCategory;
import com.heiler.ppm.importer5.core.api.PostImportStep;
import com.heiler.ppm.mail.core.MailBuilder;
import com.heiler.ppm.mail.core.MailMessage;
import com.heiler.ppm.mail.core.MessageEncoding;
import com.heiler.ppm.mail.server.MailService;
import com.heiler.ppm.problemlog.core.ProblemLog;
import com.heiler.ppm.repository.Entity;
import com.heiler.ppm.repository.core.RepositoryComponent;
import com.heiler.ppm.repository.core.RepositoryService;
import com.heiler.ppm.repository.path.FieldPath;
import com.heiler.ppm.std.core.entity.EntityDetailModel;
import com.heiler.ppm.std.core.entity.EntityProxy;
import com.heiler.ppm.std.core.entity.filter.LoadHint;
import com.heiler.ppm.std.core.entity.filter.LoadHintBuilder;

public class ArticlePostImportStep implements PostImportStep
{

  private static final Log log         = LogFactory.getLog( ArticlePostImportStep.class );

  RepositoryService        repoService = RepositoryComponent.getRepositoryService();

  @SuppressWarnings( "nls" )
  @Override
  public void executePostImportStep( ImportProfile importProfile, ImportResult importResult, ProblemLog problemLog )
  {

    Map< Entity, Map< ImportCategory, List< EntityProxy > > > entityProxyMap = importResult.getEntityProxyMap();
    if ( entityProxyMap == null )
    {
      return;
    }

    Map< ImportCategory, List< EntityProxy > > catMap = entityProxyMap.get( this.repoService.getEntityByIdentifier( ArticleCoreConst.ENTITY_ARTICLE ) );
    if ( catMap == null )
    {
      return;
    }
    List< EntityProxy > proxyList = catMap.get( ImportCategory.NEW ); // check for new items
    if ( proxyList != null )
    {
      for ( EntityProxy entityProxy : proxyList )
      {
        if ( StringUtils.equals( entityProxy.getEntityType()
                                            .getIdentifier(),
                                 ArticleCoreConst.ENTITY_TYPE_ARTICLE ) ) //check for items only
        {
          try
          {
            EntityDetailModel detailModel = entityProxy.getDetailModel( new LoadHintBuilder( entityProxy.getEntityType() ).build() );
            if ( detailModel != null )
            {
              CatalogProxy catalogProxy = ( CatalogProxy ) detailModel.getFieldValue( "catalogProxy" );
              String cat_identifier = getCatalogIdentifier( catalogProxy );

              try
              {
                detailModel.acquireWrite( null );
                EDataObject dataObject = detailModel.getDataObject();

                //generate the item idenitifer and set it in the item no field
                dataObject.set( "identifier", generateItemIdentifier( cat_identifier ) );
                detailModel.save( null );
                System.out.println("This should be checked in GitHub");
              }
              catch ( CoreException e )
              {
                e.printStackTrace();
              }
              finally
              {
                detailModel.releaseWrite();

              }
            }

          }

          catch ( CoreException e )
          {
            e.printStackTrace();
          }

        }
      }
      if ( proxyList.size() > 0 )
      {
        sendNotification( "tanmoy.chakraborty2008@gmail.com", "tanmoy.a.chakraborty@accenture.com", "Test Email",
                          "This is a test email, attachment will come soon!" );
      }
    }

  }

  /**
   * Method to send message to the server to retrieve the
   * next item number in sequence
   */
  private String generateItemIdentifier( String cat_identifier )
  {
    String latestItemNo = StringUtils.EMPTY;

    EasyMapMessage msg = new EasyMapMessage( "com.heiler.ppm.p360.customization.article.server.ItemNoHandler" ); //$NON-NLS-1$
    msg.put( "CatalogName", cat_identifier );

    try
    {
      latestItemNo = ( String ) TheNode.getNode()
                                       .sendRequest( null, NodeIdentifier.SERVER, msg, 0 );
    }
    catch ( CommunicationException e )
    {
      e.printStackTrace();

    }
    catch ( CoreException e )
    {
      e.printStackTrace();

    }
    catch ( InterruptedException e )
    {
      e.printStackTrace();
    }
    return latestItemNo;

  }

  private String getCatalogIdentifier( CatalogProxy catalogProxy )
  {
    String catalogIdentifier = null;
    if ( catalogProxy != null )
    {
      try
      {
        LoadHint loadHint = new LoadHintBuilder( LoadHint.EVERYTHING ).build();
        EntityDetailModel catalogDetailModel = catalogProxy.getDetailModel( loadHint, false );
        catalogDetailModel.acquireRead();
        try
        {
          FieldPath unitNameFieldPath = new FieldPath( "CatalogType.Identifier" );
          catalogIdentifier = ( String ) catalogDetailModel.getFieldValue( unitNameFieldPath );
        }
        finally
        {
          catalogDetailModel.releaseRead();
        }
      }
      catch ( CoreException e )
      {
        e.printStackTrace();
      }
    }
    return catalogIdentifier;
  }

  private void sendNotification( String from, String to, String subject, String message )
  {
    try
    {
      if ( from != null && to != null && subject != null && message != null )
      {
        MailBuilder mailBuilder = new MailBuilder( com.heiler.ppm.mail.core.MessageType.HTML, MessageEncoding.UTF8,
                                                   from, to );

        MailMessage mailMsg = mailBuilder.subject( subject )
                                         .message( message )
                                         .build();
        Properties props = new Properties();
        props.put( "mail.smtp.host", "smtp.gmail.com" ); //SMTP Host
        props.put( "mail.smtp.port", "587" ); //TLS Port
        props.put( "mail.smtp.auth", "true" ); //enable authentication
        props.put( "mail.smtp.starttls.enable", "true" ); //enable STARTTLS

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator()
        {
          //override the getPasswordAuthentication method
          @Override
          protected PasswordAuthentication getPasswordAuthentication()
          {
            return new PasswordAuthentication( from, "bolbona#1" );
          }
        };
        Session session = Session.getInstance( props, auth );

        MailService.getInstance()
                   .send( mailMsg );
      }
    }
    catch ( CoreException e )
    {
      log.error( e );

    }
    catch ( Exception e )
    {
      log.error( e );

    }
  }

}
