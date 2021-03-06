/**
 * 
 */
package org.sakaiproject.tool.resetpass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.Period;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.ToolManager;

import uk.org.ponder.localeutil.LocaleGetter;
import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.messageutil.TargettedMessageList;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UIInput;
import uk.org.ponder.rsf.components.UIMessage;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UIVerbatim;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCase;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCaseReporter;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.DefaultView;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;


/**
 * @author dhorwitz
 *
 */
public class FormProducer implements ViewComponentProducer, DefaultView,NavigationCaseReporter {

	public static final String VIEW_ID = "form";
	
	/* (non-Javadoc)
	 * @see uk.org.ponder.rsf.view.ViewComponentProducer#getViewID()
	 */
	public String getViewID() {
		return VIEW_ID;
	}

	MessageLocator messageLocator;
	public void setMessageLocator(MessageLocator ml) {
		messageLocator = ml;
	}
	
	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(ServerConfigurationService s) {
		this.serverConfigurationService = s;
	}
	
	private TargettedMessageList tml;
	  
	  public void setTargettedMessageList(TargettedMessageList tml) {
		    this.tml = tml;
	  }
  
	private ToolManager toolManager;
	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

	private static LocaleGetter localeGetter;
	public void setLocaleGetter(LocaleGetter localeGetter)
	{
		this.localeGetter = localeGetter;
	}

	private Locale getLocale()
	{
		return localeGetter.get();
	}
	
	/* (non-Javadoc)
	 * @see uk.org.ponder.rsf.view.ComponentProducer#fillComponents(uk.org.ponder.rsf.components.UIContainer, uk.org.ponder.rsf.viewstate.ViewParameters, uk.org.ponder.rsf.view.ComponentChecker)
	 */
	public void fillComponents(UIContainer tofill, ViewParameters viewParms,
			ComponentChecker comp) {
		// TODO Auto-generated method stub

		
		if (tml!=null) {
			if (tml.size() > 0) {

		    	for (int i = 0; i < tml.size(); i ++ ) {
		    		UIBranchContainer errorRow = UIBranchContainer.make(tofill,"error-row:");
		    		if (tml.messageAt(i).args != null ) {	    		
		    			UIVerbatim.make(errorRow, "error", messageLocator.getMessage(tml.messageAt(i).acquireMessageCode(), (String[])tml.messageAt(i).args[0]));
		    		} else {
		    			UIVerbatim.make(errorRow, "error", messageLocator.getMessage(tml.messageAt(i).acquireMessageCode()));
		    		}
		    		
		    	}
		    }
		}
		// Get the instructions from the tool placement.
		Placement placement = toolManager.getCurrentPlacement();
		if (placement != null) {
			String instuctions = placement.getConfig().getProperty("instructions");
			if (instuctions != null && instuctions.length() > 0) {
				UIVerbatim.make(tofill, "instructions", instuctions);
			}
		} else {
			String[] args = new String[1];
			args[0]=serverConfigurationService.getString("ui.service", "Sakai Based Service");
			UIVerbatim.make(tofill,"main",messageLocator.getMessage("mainText", args));
		}
		UIForm form = UIForm.make(tofill,"form");
		boolean skipExpirationTime=true;
		String expirationTime=serverConfigurationService.getString("accountValidator.maxPasswordResetMinutes");
		if (expirationTime != null && !"".equals(expirationTime))
		{
			try
			{
				int totalMinutes=Integer.parseInt(expirationTime);

				skipExpirationTime=false;
				UIOutput.make( form, "output", messageLocator.getMessage("explanation", new Object[]{getFormattedMinutes(totalMinutes)} ));
			}
			catch (NumberFormatException nfe)
			{

			}
		}
		if (skipExpirationTime)
		{
			UIOutput.make(form, "output", "");
		}
		UIInput.make(form,"input","#{userBean.email}");

		boolean validatingAccounts = serverConfigurationService.getBoolean( "siteManage.validateNewUsers", false );
		if ( validatingAccounts )
		{
			UICommand.make( form, "submit", UIMessage.make( "postForm2" ), "#{formHandler.processAction}" );
		}
		else
		{
			UICommand.make( form, "submit", UIMessage.make( "postForm" ), "#{formHandler.processAction}" );
		}
	}

	/**
	 * Converts some number of minutes into a presentable String'
	 * ie for English:
	 * 122 minutes  ->      2 hours 2 minutes
	 * 121 minutes  ->      2 hours 1 minute
	 * 120 minutes  ->      2 hours
	 * 62 minutes   ->      1 hour 2 minutes
	 * 61 minutes   ->      1 hour 1 minute
	 * 60 minutes   ->      1 hour
	 * 2 minutes    ->      2 minutes
	 * 1 minutes    ->      1 minute
	 * 0 minutes    ->      0 minutes
	 * Works with other languages too.
	 * @param totalMinutes some number of minutes
	 * @return a presentable String representation of totalMinutes
	 */
	public String getFormattedMinutes(int totalMinutes)
	{
		// Create a joda time period (takes milliseconds)
		Period period = new Period(totalMinutes*60*1000);
		// format the period for the locale
		/* 
		 * Covers English, Danish, Dutch, French, German, Japanese, Portuguese, and Spanish. 
		 * To translate into others, see http://joda-time.sourceforge.net/apidocs/org/joda/time/format/PeriodFormat.html#wordBased(java.util.Locale)
		 * (ie. put the properties mentioned in http://joda-time.sourceforge.net/apidocs/src-html/org/joda/time/format/PeriodFormat.html#line.94 into the classpath resource bundle)
		 */
		PeriodFormatter periodFormatter = PeriodFormat.wordBased(getLocale());
		return periodFormatter.print(period);
	}
	
	  public List<NavigationCase> reportNavigationCases() {
		    List<NavigationCase> togo = new ArrayList<NavigationCase>(); // Always navigate back to this view.
		    togo.add(new NavigationCase(null, new SimpleViewParameters(VIEW_ID)));
		    togo.add(new NavigationCase("Success", new SimpleViewParameters(ConfirmProducer.VIEW_ID)));
		    return togo;
	  }
	  
}
