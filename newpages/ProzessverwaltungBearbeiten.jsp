<%@ page session="false" contentType="text/html;charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://jsftutorials.net/htmLib" prefix="htm"%>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="x"%>
<%@ taglib uri="https://ajax4jsf.dev.java.net/ajax" prefix="a4j"%>

<%--
  ~ This file is part of the Goobi Application - a Workflow tool for the support of
  ~ mass digitization.
  ~
  ~ Visit the websites for more information.
  ~     - http://gdz.sub.uni-goettingen.de
  ~     - http://www.goobi.org
  ~     - http://launchpad.net/goobi-production
  ~
  ~ This program is free software; you can redistribute it and/or modify it under
  ~ the terms of the GNU General Public License as published by the Free Software
  ~ Foundation; either version 2 of the License, or (at your option) any later
  ~ version.
  ~
  ~ This program is distributed in the hope that it will be useful, but WITHOUT ANY
  ~ WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  ~ PARTICULAR PURPOSE. See the GNU General Public License for more details. You
  ~ should have received a copy of the GNU General Public License along with this
  ~ program; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
  ~ Suite 330, Boston, MA 02111-1307 USA
  --%>

<%-- ########################################

							Prozess bearbeiten

	#########################################--%>
<a4j:keepAlive beanName="ProzessverwaltungForm" />
<html>
<f:view locale="#{SpracheForm.locale}">
	<%@include file="inc/head.jsp"%>
	<body>

	<htm:table cellspacing="5" cellpadding="0" styleClass="layoutTable" align="center">
		<%@include file="inc/tbl_Kopf.jsp"%>
		<htm:tr>
			<%@include file="inc/tbl_Navigation.jsp"%>
			<htm:td valign="top" styleClass="layoutInhalt">

				<%-- ++++++++++++++++     Inhalt      ++++++++++++++++ --%>
				<h:form id="proceditform">
					<%-- Breadcrumb --%>
					<h:panelGrid width="100%" columns="1" styleClass="layoutInhaltKopf">
						<h:panelGroup>
							<h:commandLink value="#{msgs.startseite}" action="newMain" />
							<f:verbatim> &#8250;&#8250; </f:verbatim>
							<h:commandLink value="#{msgs.prozessverwaltung}" action="ProzessverwaltungAlle" />
							<f:verbatim> &#8250;&#8250; </f:verbatim>
							<h:outputText value="#{msgs.einenNeuenProzessAnlegen}" rendered="#{ProzessverwaltungForm.myProzess.id == null}" />
							<h:outputText value="#{msgs.prozessDetails}" rendered="#{ProzessverwaltungForm.myProzess.id != null}" />
						</h:panelGroup>
					</h:panelGrid>

					<htm:table border="0" align="center" width="100%" cellpadding="15">
						<htm:tr>
							<htm:td>
								<htm:h3>
									<h:outputText value="#{msgs.einenNeuenProzessAnlegen}" rendered="#{ProzessverwaltungForm.myProzess.id == null}" />
									<h:outputText value="#{msgs.prozessDetails}" rendered="#{ProzessverwaltungForm.myProzess.id != null}" />
								</htm:h3>

								<%-- globale Warn- und Fehlermeldungen --%>
								<h:messages globalOnly="true" errorClass="text_red" infoClass="text_blue" showDetail="true" showSummary="true" tooltip="true" />

								<%-- Prozessdetails --%>
								<%@include file="inc_Prozessverwaltung/prozess_box_Prozessdetails.jsp"%>

								<f:subview id="sub001" rendered="#{ProzessverwaltungForm.myProzess.id != null}">
									<%-- Schritte --%>
									<%@include file="inc_Prozessverwaltung/prozess_box_Schritte.jsp"%>

									<%-- Vorlagen --%>
									<f:subview id="subVorlage" rendered="#{ProzessverwaltungForm.myProzess.vorlagenSize != 0}">
										<%@include file="inc_Prozessverwaltung/prozess_box_Vorlagen.jsp"%>
									</f:subview>
	
									<%-- Werkstuecke --%>
									<f:subview id="subWerk" rendered="#{ProzessverwaltungForm.myProzess.werkstueckeSize != 0}">
										<%@include file="inc_Prozessverwaltung/prozess_box_Werkstuecke.jsp"%>
									</f:subview>

									<%-- Prozesseigenschaften --%>
									<%@include file="inc_Prozessverwaltung/prozess_box_Prozesseigenschaften.jsp"%>

								</f:subview>

							</htm:td>
						</htm:tr>
					</htm:table>
				</h:form>
				<%-- ++++++++++++++++    // Inhalt      ++++++++++++++++ --%>

			</htm:td>
		</htm:tr>
		<%@include file="inc/tbl_Fuss.jsp"%>
	</htm:table>



	</body>
</f:view>

</html>