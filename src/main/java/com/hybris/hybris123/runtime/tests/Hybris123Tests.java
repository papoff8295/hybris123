package com.hybris.hybris123.runtime.tests;

/*
 * © 2017, © 2018 SAP SE or an SAP affiliate company.
 * All rights reserved.
 * Please see http://www.sap.com/corporate-en/legal/copyright/index.epx for additional trademark information and notices.
 * This sample code is provided for the purpose of these guided tours only and is not intended to be used in a production environment.
 */

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.hybris.hybris123.runtime.tests.Hybris123Tests.SeleniumHelper.*;



/*
 * © 2017, © 2018 SAP SE or an SAP affiliate company.
 */





/**
 * These tests check on your progress thru hybris123
 */
public class Hybris123Tests {
    private static final Logger LOG = LoggerFactory.getLogger(Hybris123Tests.class);
    private static boolean WAITONFAIL = false;


/*
SeleniumHelper.java */






    public static class SeleniumHelper {
        private static final Logger LOG = LoggerFactory.getLogger(SeleniumHelper.class);
        private static WebDriver dvr = null;

        private static final String yUSERNAME="admin";
        private static final String yPASSWORD="nimda";

        private static Wait<WebDriver> wait;
        private static Wait<WebDriver> longWait;
        private static int PAUSE_MS = 2000;
        private static int PAUSE_FOR_SERVER_START_MS  = 120000;
        private static int PAUSE_BETWEEN_KEYS_MS = 50;

        private static int NORMAL_WAIT_S = 10;
        private static int LONG_WAIT_S = 120;
        private static int POLLING_RATE_S = 2;
        private static boolean WINDOWS = (System.getProperty("os.name")!=null)? System.getProperty("os.name").toLowerCase().contains("windows") : false;
        private static boolean OSX = (System.getProperty("os.name")!=null)? System.getProperty("os.name").toLowerCase().contains("mac") : false;


        public static boolean canLoginToHybrisCommerce()  {
            try {

                waitForConnectionToOpen("https://localhost:9002/login.jsp", PAUSE_FOR_SERVER_START_MS);
                getDriver().get("https://localhost:9002/login.jsp");
                pauseMS();
                WebElement usernameElem =  findElement(By.name("j_username"));
                WebElement passwordElem =  findElement(By.name("j_password"));

                clearField( usernameElem );
                usernameElem.sendKeys( yUSERNAME);
                clearField( passwordElem );
                passwordElem.sendKeys( yPASSWORD );
                pauseMS();
                passwordElem.submit();
                Assert.assertTrue( waitFor("div", "Memory overview"));
                return true;
            } catch (Exception e) {
                StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                String callingMethod = stackTraceElements[2].getMethodName();
                Hybris123Tests.fail(callingMethod, "Connect Exception: " + e.getMessage());
            }
            return false;
        }

        public static boolean loginToBackOffice(String ... language) {
            try {
                pauseMS(PAUSE_MS);
                // Allow time for server to start
                waitForConnectionToOpen("https://localhost:9002/backoffice", PAUSE_FOR_SERVER_START_MS);
                getDriver().get("https://localhost:9002/backoffice");
                if (language.length==0){
                    Select s = new Select(findElement(By.xpath("//select")));
                    s.selectByVisibleText("English");
                }
                if (language.length==1){
                    Select s = new Select(findElement(By.xpath("//select")));
                    s.selectByVisibleText(language[0]);
                }
                pauseMS(PAUSE_MS);

                WebElement un = findElement(By.name("j_username"));
                clearField( un, yUSERNAME+Keys.TAB );

                WebElement pwd = findElement(By.name("j_password"));
                clearField( pwd, yPASSWORD+Keys.TAB );
                pauseMS(PAUSE_MS);
                pwd.submit();
                return true;
            } catch (Exception e) {
                String callingMethod =Thread.currentThread().getStackTrace()[2].getMethodName();
                Hybris123Tests.fail(callingMethod, "Connect Exception");
            }
            return false;
        }

        private static void waitForThenDoBackofficeSearch(String search, String xp){
            waitUntilElement(By.xpath(xp));
            pauseMS(PAUSE_MS);
            sendKeysSlowly(findElement(By.xpath(xp)), search+Keys.RETURN);
            waitUntilElement(By.xpath("//button[contains(@class, 'yw-textsearch-searchbutton')]"));
            pauseMS(PAUSE_MS);
            scrollToThenClick( findElement(By.xpath(xp)) );
        }

        public static void waitForThenDoBackofficeSearch(String search){
            pauseMS(PAUSE_MS);
            if (VersionHelper.is60()){
                if (search.length()==0) {// 6.0 Default search does not work; need to expand it like this
                    waitForthenScrollToThenClick( "//button[contains(@class, 'yw-toggle-advanced-search')]" );
                    pauseMS(PAUSE_MS);
                    waitForthenScrollToThenClick( "//button[contains(@class, 'yw-textsearch-searchbutton')]" );
                }
                else
                    waitForThenDoBackofficeSearch(search, "//input[contains(@class, 'yw-textsearch-searchbox')]");
            }
            else
                waitForThenDoBackofficeSearch(search, "//input[contains(@class, 'z-bandbox-input')]");
            pauseMS(PAUSE_MS);
        }

        private static void waitForthenScrollToThenClick( String xpath ) {
            WebElement we = waitUntilElement(By.xpath(xpath));
            scrollToThenClick(we);
        }

        private static void scrollToThenClick( WebElement e) {
            ((JavascriptExecutor) getDriver()).executeScript("arguments[0].scrollIntoView(true);", e);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
            }
            e.click();
        }

        public static WebElement waitUntilElement(By by) {
            return wait.until( (WebDriver webDriver)  -> findElement(by));
        }

        public static List<WebElement> waitUntilElements(By by) {
            return wait.until( (WebDriver webDriver)  -> findElements(by));
        }

        public static void waitForThenClickOkInAlertWindow(){
            longWait.until(ExpectedConditions.alertIsPresent());
            getDriver().switchTo().alert().accept();
        }

        public static WebElement waitForConstraintsMenu( ){
            try{
                wait.until((WebDriver webDriver)  -> findElements(By.xpath("//span[@class='z-tree-icon']")).size()  == 3);
            }catch(Exception e){
                // 6.1 and before requires clicking an extra top level menu entry "Constraint"
                waitForthenScrollToThenClick("//span[@class='z-tree-icon']");
                wait.until((WebDriver webDriver)  -> findElements(By.xpath("//span[@class='z-tree-icon']")).size()  == 3);
            }
            List<WebElement>we = findElements(By.xpath("//span[@class='z-tree-icon']"));
            scrollToThenClick(we.get(1));
            return we.get(1);
        }

        public static void waitForTagXWithAttributeYWithValueZThenClick( String tag, String att, String value){
            try{
                pauseMS(PAUSE_MS);
                waitForthenScrollToThenClick( "//"+tag+"[@"+att+"='"+value+"']" );
            }catch(Exception e){
                LOG.error(e.getMessage());
            }
        }

        public static WebElement findElement(By by) {
            return getDriver().findElement(by);
        }

        public static List<WebElement> findElements(By by) {
            return getDriver().findElements(by);
        }

        public static void waitForGroovyWindowThenSubmitScript(String gs ){
            // Rollback to Commit
            pauseMS(2000);
            waitForthenScrollToThenClick("//label[@for='commitCheckbox']");
            WebElement queryInput = waitUntilElement(By.xpath("//div[contains(@class, 'CodeMirror')]"));
            pauseMS(2000);
            JavascriptExecutor js = (JavascriptExecutor) dvr;

            gs = gs.replaceAll("\\n", "\\\\"+"n");

            js.executeScript("arguments[0].CodeMirror.setValue('"+gs+"');", queryInput);
            // Note some users have noted that they need the following line instead of the previous one
            //js.executeScript("arguments[0].CodeMirror.setValue(\""+gs+"\");", queryInput);

            waitForthenScrollToThenClick( "//button[@id='executeButton']");
            pauseMS(4000);

        }

        public static void waitForFlexQueryFieldThenSubmit(String fq){
            WebElement localSel = waitUntilElement(By.id("locale1"));
            new Select(localSel).selectByVisibleText("en");

            WebElement queryInput = waitUntilElement(By.xpath("//div[contains(@class, 'CodeMirror')]"));
            JavascriptExecutor js = (JavascriptExecutor) dvr;
            js.executeScript("arguments[0].CodeMirror.setValue('"+fq+"');", queryInput);
            scrollToThenClick( findElement(By.xpath("//button[@id='buttonSubmit1']")));
            pauseMS(PAUSE_MS);
            scrollToThenClick( findElement(By.xpath("//a[@id='nav-tab-3']")));
        }

        public static boolean waitForText( String text ){
            wait.until(webDriver -> webDriver.findElement(By.tagName("body")).getText().contains(text));
            return true;
        }

        public  static boolean waitFor(String tag, String text) {
            try {
                waitUntilElement(By.xpath("//"+tag+"[text()='"+text+"']"));
                return true;
            }
            catch(Exception e) {
                if (text.equals("Import finished successfully")){
                    waitUntilElement(By.xpath("//"+tag+"[text()='Import wurde erfolgreich abgeschlossen']"));
                    return true;
                }
                throw new NoSuchElementException("Text not found in  waitFor: "+text);
            }
        }

        public static boolean waitForTagContaining(String tag, String text) {
            waitUntilElement(By.xpath("//"+tag+"[contains(text(),'"+text+"')]"));
            return true;
        }

        public static boolean waitForImageWithTitleThenClick( String title ){
            waitForthenScrollToThenClick("//img[@title='"+title+"']");
            return true;
        }

        public static boolean waitForValidImage(  ){
            wait.until( webDriver -> findElements(By.tagName("img")).size() > 0 );
            String src = findElements(By.tagName("img")).get(0).getAttribute("src");
            return src.contains("media");
        }

        public static boolean waitForThenUpdateInputField(String from, String to) {
            WebElement e = waitUntilElement(By.xpath("//input[@value='"+from+"']"));
            clearField( e );

            sendKeysSlowly(e, to);
            e.sendKeys(Keys.RETURN);
            return true;
        }


        public static boolean waitForThenClick(String tag, String text) {
            pauseMS(PAUSE_MS);
            try {
                waitForthenScrollToThenClick("//"+tag+"[text()='"+text+"']");
            }
            catch(Exception e) {
                LOG.debug("Not found " +"//"+tag+"[text()='"+text+"']. If 6.2 or eariler and span, will try for div");
            }

            if (tag.equals("span") && VersionHelper.is60() || VersionHelper.is61() || VersionHelper.is62() ){
                try {
                    waitForthenScrollToThenClick("//div[text()='"+text+"']");
                }
                catch(Exception e) {
                    LOG.debug("Not found " +"//div[text()='"+text+"']");
                }
            }
            return true;
        }

        public static boolean waitForValue(String tag, String text) {
            waitUntilElement(By.xpath("//"+tag+"[@value='"+text+"']"));
            return true;
        }

        public static Boolean waitForExtensionListing(String extensionName ){
            return waitUntilElement(By.xpath("//td[@data-extensionname='"+extensionName+"']")) != null;
        }

        public static void waitForThenClickMenuItem(String menuItem) {
            pauseMS(PAUSE_MS);
            waitUntilElement(By.xpath("//tr[@title='" + menuItem + "']"));
            pauseMS(PAUSE_MS);
            waitForthenScrollToThenClick("//span[text()='" + menuItem + "']");
            pauseMS(PAUSE_MS);
        }

        public static void waitForThenClickDotsBySpan(String text){
            pauseMS(PAUSE_MS);
            try {
                waitFor("span",text);
            }
            catch(Exception e){ // 6.2, 6.1 expect a div rather than a span
                waitFor("div",text);
            }
            WebElement dots = findElements(By.xpath("//td[contains(@class, 'ye-actiondots')]")).get(2) ;
            scrollToThenClick( dots );
        }

        public static void waitForThenAndClickSpan(final String spanText ){ // For 6.2 Divs versus Spans
            pauseMS(PAUSE_MS);
            try {
                waitForthenScrollToThenClick("//span[text()='" + spanText + "']");
            }
            catch(Exception e){
                if (spanText.equals("Concert")) {// 6.2 expects div with Konzert rather than span with Concert
                    waitForthenScrollToThenClick("//div[text()='Konzert']");
                }
                else {
                    waitForthenScrollToThenClick("//div[text()='" + spanText + "']");
                }
            }
        }

        public static void waitForThenAndClickSpan(String spanText, String ... spanOptionalText) {
            pauseMS(PAUSE_MS);
            try {
                waitForthenScrollToThenClick("//span[text()='" + spanText + "']");
            }
            catch(Exception e){
                if (spanOptionalText!=null){
                    waitForthenScrollToThenClick("//span[text()='" + spanText + "'] | //span[text()='" + spanOptionalText[0] + "'] " );
                }
            }
        }

        public static void waitForThenAndClickDiv(String divText, String ... divOptionalText) {
            pauseMS(PAUSE_MS);
            try {
                waitForthenScrollToThenClick("//div[text()='" + divText + "']");
            }
            catch(Exception e){
                if (divOptionalText!=null){
                    waitForthenScrollToThenClick("//div[text()='" + divText + "'] | //div[text()='" + divOptionalText[0] + "'] " );
                }
            }
        }

        public static void waitForThenClickButtonWithText(String buttonText) {
            pauseMS(PAUSE_MS);
            waitForthenScrollToThenClick("//button[text()='" + buttonText + "']");
        }

        public static  void waitForInitToComplete() {
            pauseMS(PAUSE_MS);
            scrollToBottom();
            longWait.until(webDriver -> findElement(By.xpath("//a[text()='Continue...']")));
            scrollToBottom();
            scrollToThenClick( findElement(By.xpath("//a[text()='Continue...']")));
        }

        private static void scrollToBottom(){
            int NUM = 4;
            for (int i=0; i < NUM; i ++) {
                new Actions(getDriver()).sendKeys(Keys.PAGE_DOWN).perform();
                pauseMS(100);
            }
        }
        public static  void waitForNoificationToClose() {
            pauseMS();
        }

        public static  void waitForAllInputFields(int n){
            wait.until( webDriver ->  findElements(By.xpath("//input[@type='text']")).size()>=n );
            pauseMS(PAUSE_MS);
        }


        public static  void navigateTo(String url){
            getDriver().navigate().to(url);
            pauseMS(PAUSE_MS);
        }

        public static String getTitle(){
            return getDriver().getTitle();
        }

        public static String getXMLFromPage(String page){
            navigateTo(page);
            String content = getDriver().getPageSource();
            content = content.replaceAll("\n", "");
            return content;
        }

        public static void submitImpexScript(String impex){
            WebElement queryInput = findElement(By.xpath("//div[contains(@class, 'CodeMirror')]")) ;
            JavascriptExecutor js = (JavascriptExecutor) dvr;
            impex = impex.replaceAll("\\n", "\\\\n");
            js.executeScript("arguments[0].CodeMirror.setValue('"+impex+"');", queryInput);

            try {
                waitForthenScrollToThenClick("//input[@value='Import content']");
                return;
            }
            catch (Exception e){
                waitForthenScrollToThenClick("//input[@value='Inhalt importieren']");
            }
        }



        public static void setDriver( WebDriver wd)
        {
            dvr = wd;
        }


        public static WebDriver peakDriver() {
            return dvr;
        }

        private static int parseString(String s){
            try {
                return Integer.parseInt(s);
            }
            catch (Exception e){
                return -1;
            }
        }

        public static WebDriver getDriver() {
            if (dvr != null)
                return dvr;


            LOG.debug("In getdriver");
            // due to issue 232 in WebDriverManager. forceDownload removes the cache functionality
            WebDriverManager.chromedriver().version("93.0.4577.63").forceDownload().targetPath("resources/selenium").setup();
            //WebDriverManager.chromedriver().targetPath("resources/selenium").setup();

            if (WINDOWS) {
                dvr = new ChromeDriver();
                dvr.manage().window().setSize(new Dimension(1024,768));
            } else {
                // add --no-sandbox to prevent unknown WebDriver error
                List<String> optionArguments = new ArrayList<>();
                optionArguments.add("window-size=1024,768");
                optionArguments.add("--no-sandbox");
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments(optionArguments);

                int seleniumPort = parseString( System.getProperty("selenium.port") );
                if (seleniumPort!=-1){
                    LOG.debug("Opening chromedriver on port" + seleniumPort);
                    ChromeDriverService cds =
                            new ChromeDriverService.Builder().usingDriverExecutable(
                                    new File( "./chromedriver"))
                                    .usingPort(seleniumPort)
                                    .build();

                    dvr = new ChromeDriver(cds, chromeOptions);
                    dvr.manage().window().setSize(new Dimension(1024,768));
                }
                else {
                    dvr = new ChromeDriver(chromeOptions);
                    dvr.manage().window().setSize(new Dimension(1024,768));
                }
            }

            wait = new FluentWait<WebDriver>(dvr).withTimeout(NORMAL_WAIT_S, TimeUnit.SECONDS)
                    .pollingEvery(POLLING_RATE_S, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);

            longWait = new FluentWait<WebDriver>(dvr).withTimeout(LONG_WAIT_S, TimeUnit.SECONDS)
                    .pollingEvery(POLLING_RATE_S, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);

            return dvr;
        }

        public static boolean checkTestSuiteXMLMatches( String s ){
            try {
                String fileContents = FileHelper.getContents("../hybris/log/junit/TESTS-TestSuites.xml").replace("\n", "").replace("\r", "");
                boolean match =  fileContents.matches(s);
                if (!match)
                    System.out.println("checkTestSuiteXMLMatches failed: " + fileContents);
                return match;
            } catch (IOException e) {
                Hybris123Tests.fail("Regex not found:" + s);
            }
            return false;
        }

        public static String callCurl(String... curl){
            byte[] bytes = new byte[100];
            StringBuffer response = new StringBuffer();
            try {
                ProcessBuilder pb = new ProcessBuilder( curl);
                Process p = pb.start();
                InputStream is = p.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                while ( bis.read(bytes, 0, 100) != -1) {
                    for (byte b : bytes) {
                        response.append((char)b);
                    }
                    Arrays.fill(bytes, (byte) 0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response.toString();
        }

        public static String getMethodName(){
            final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
            int i = 2;
            while (!ste[i].getMethodName().startsWith("test") &&
                    !ste[i].getMethodName().startsWith("gitRepoOk")  &&
                    !ste[i].getMethodName().startsWith("loginAndCheckForConcertToursExtension"))
                i++;
            return  ste[i].getMethodName();
        }

        public static void modifyABandToHaveNegativeAlbumSales() {
            waitForThenClickMenuItem("System");
            waitForThenClickMenuItem("Types");
            waitForThenDoBackofficeSearch("Band");
            waitForThenClick("span","Band");  // 6.2 expects div
            waitForImageWithTitleThenClick("Search by type");
            waitForThenDoBackofficeSearch(""); // 6.2
            waitForThenClick("span","The Quiet");// 6.2 expects div
            waitForThenUpdateInputField("1200", "-1200");
            waitForThenClick("button","Save");
        }

        public static void tryToViolateTheNewConstraint() {
            waitForThenClickMenuItem("Types");
            waitForThenDoBackofficeSearch("Band");
            waitForThenClick("span","Band");// 6.2 expects div
            waitForImageWithTitleThenClick("Search by type");
            waitForThenDoBackofficeSearch(""); // 6.2
            waitForThenClick("span","The Quiet"); // 6.2 expects div
            waitForThenUpdateInputField("1200", "-1200");
            waitForThenClick("button","Save");
            waitFor("div","You have 1 Validation Errors");
        }

        public static void tryToViolateTheNewCustomConstraint() {
            waitForThenClickMenuItem("Types");
            waitForThenDoBackofficeSearch("Band"+Keys.RETURN);
            waitForThenClick("span","Band");// 6.2 expects div
            waitForImageWithTitleThenClick("Search by type");
            waitForThenDoBackofficeSearch("");//6.2
            waitForThenClick("span","The Quiet");// 6.2 expects div
            waitForThenUpdateInputField("English choral society specialising in beautifully arranged, soothing melodies and songs", "Lorem Ipsum"+Keys.RETURN);
            waitForThenClick("button","Save");
            waitFor("div","You have 1 Validation Errors");
        }

        public static void reloadConstraints() {
            waitForThenClickMenuItem("Constraints");
            waitForTagXWithAttributeYWithValueZThenClick("img","title","Reload validation engine");
            waitForThenClick("button","Yes");
            pauseMS();
        }

        public static void pauseMS(long ... pause) {
            try {
                if (pause.length==0)
                    Thread.sleep(6000);
                else
                    Thread.sleep(pause[0]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        private static void clearField(WebElement elem){
            clearField(elem, "");
        }


        private static void sendKeysSlowly(WebElement elem, String input){
            for(int i = 0; i < input.length(); i++){
                elem.sendKeys(input.charAt(i) + "");
                pauseMS(PAUSE_BETWEEN_KEYS_MS);
            }
        }
        private static void clearField(WebElement elem, String newInput){
            elem.clear();
            pauseMS(PAUSE_BETWEEN_KEYS_MS);
            elem.sendKeys(newInput);
        }

        public static void addNewMinConstraint(String id) {
            if (VersionHelper.is61() || VersionHelper.is60()) {
                addNewMinConstraint61(id);
                return;
            }

            waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");
            waitForConstraintsMenu();
            waitForTagXWithAttributeYWithValueZThenClick("tr","title","Min constraint");
            waitForAllInputFields(20);

            WebElement idInputField = findElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
            clearField(idInputField, id);

            WebElement minimalValueField = findElements(By.xpath("//span[text()='Minimal value:']/following::input[1]")).get(0);
            clearField(minimalValueField, "0");

            WebElement dots = waitUntilElements(By.xpath("//span[text()='Enclosing Type:' or text()='Composed type to validate:']/following::i[1]")).get(0);
            scrollToThenClick(dots);
            pauseMS(PAUSE_MS);
            WebElement identifierField = waitUntilElements(By.xpath("//span[text()='Identifier']/following::input[2]")).get(0);
            identifierField.sendKeys("Band"+Keys.RETURN);
            waitForThenClick("span","Band");// 6.2 expects div
            waitForThenClick("button","Select (1)");
            pauseMS(PAUSE_MS);

            WebElement attributeDescField = findElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::input[1]")).get(0);
            sendKeysSlowly(attributeDescField,  "album sales");
            attributeDescField.sendKeys(Keys.DOWN);

            waitForThenClick("span","Band [Band] -> album sales [albumSales]");
            scrollToBottom();
            waitForThenClick("button","Done");
            waitForNoificationToClose();

            // Add a message to the the new min constraint
            waitForThenDoBackofficeSearch(id);
            waitForThenClick("span", id);// 6.2 expects div
            waitForTagXWithAttributeYWithValueZThenClick("button","class","yw-expandCollapse z-button");
            pauseMS(PAUSE_MS);

            WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
            sendKeysSlowly(errorMessageField, "Album sales must be > 0. Do pay attention."+Keys.RETURN);
            waitForThenClick("button","Save");
            waitForNoificationToClose();
        }

        public static void addNewMinConstraint61(String id) {
            waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");

            waitForConstraintsMenu();

            waitForTagXWithAttributeYWithValueZThenClick("tr","title","Min constraint");
            pauseMS(1000);

            WebElement idInputField = findElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
            clearField(idInputField, id);

            WebElement minimalValueField = findElements(By.xpath("//span[text()='Minimal value:']/following::input[1]")).get(0);
            clearField(minimalValueField, "0");

            WebElement enclosingTypeField = findElements(By.xpath("//span[text()='Enclosing Type:']/following::input[1]")).get(0);
            enclosingTypeField.sendKeys("Band"+Keys.RETURN);

            pauseMS(1000);
            WebElement dots = findElements(By.xpath("//span[text()='Enclosing Type:']/following::i[1]")).get(0);
            scrollToThenClick( dots);

            waitForThenClick("span","Band [Band]");// 6.2 expects div
            pauseMS(PAUSE_MS);

            WebElement attributeDescField = findElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::input[1]")).get(0);
            sendKeysSlowly(attributeDescField,  "album sales");
            attributeDescField.sendKeys(Keys.DOWN);

            waitForThenClick("span","Band [Band] -> album sales [albumSales]");
            scrollToBottom();
            waitForThenClick("button","Done");
            waitForNoificationToClose();

            // Add a message to the the new min constraint
            waitForThenDoBackofficeSearch(id);
            waitForThenClick("span", id);// 6.2 expects div
            waitForTagXWithAttributeYWithValueZThenClick("button","class","yw-expandCollapse z-button");
            pauseMS(PAUSE_MS);

            WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
            sendKeysSlowly(errorMessageField, "Album sales must be > 0. Do pay attention."+Keys.RETURN);
            waitForThenClick("button","Save");
            waitForNoificationToClose();
        }

        public static void addNewCustomConstraint(String id) {
            if (VersionHelper.is61() || VersionHelper.is60()) {
                addNewCustomConstraint61(id);
                return;
            }

            waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");

            waitForConstraintsMenu();

            scrollToBottom();
            waitForTagXWithAttributeYWithValueZThenClick("tr","title","NotLoremIpsumConstraint");
            waitForAllInputFields(19);
            scrollToBottom();
            List<WebElement> l = findElements(By.xpath("//input[@type='text']"));
            scrollToBottom();

            WebElement idInputField = waitUntilElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
            clearField(idInputField, id+Keys.RETURN);


            // Set Band [Bands]
            WebElement dots = waitUntilElements(By.xpath("//span[text()='Enclosing Type:']/following::i[1]")).get(0);
            scrollToThenClick( dots);
            pauseMS(PAUSE_MS);
            WebElement identifierField = waitUntilElements(By.xpath("//span[text()='Identifier']/following::input[2]")).get(0);
            identifierField.sendKeys("Band"+Keys.RETURN);
            waitForThenClick("span","Band");// 6.2 expects div
            waitForThenClick("button","Select (1)");
            pauseMS(PAUSE_MS);

            waitForAllInputFields(19);


            dots = waitUntilElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::i[1]")).get(0);
            scrollToThenClick( dots);
            pauseMS(PAUSE_MS);
            waitForThenClick("span","history");// 6.2 expects div
            waitForThenClick("button","Select (1)");
            pauseMS(PAUSE_MS);

            waitForThenClick("button","Done");
            waitForNoificationToClose();

            // Add a message to the the custom constraint
            waitForThenDoBackofficeSearch(id);
            waitForThenClick("span", id);// 6.2 expects div
            waitForTagXWithAttributeYWithValueZThenClick("button","class","yw-expandCollapse z-button");
            WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
            sendKeysSlowly(errorMessageField, "No Lorem Ipsum");
            waitForThenClick("button","Save");
            waitForNoificationToClose();
        }

        public static void addNewCustomConstraint61(String id) {
            waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");

            waitForConstraintsMenu();

            waitForTagXWithAttributeYWithValueZThenClick("tr","title","NotLoremIpsumConstraint");
            pauseMS(1000);

            WebElement idInputField = findElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
            clearField(idInputField, id);

            WebElement enclosingTypeField = findElements(By.xpath("//span[text()='Enclosing Type:']/following::input[1]")).get(0);
            enclosingTypeField.sendKeys("Band"+Keys.RETURN);
            WebElement dots = findElements(By.xpath("//span[text()='Enclosing Type:']/following::i[1]")).get(0);
            scrollToThenClick( dots);
            pauseMS(PAUSE_MS);

            waitForThenClick("span","Band [Band]");// 6.2 expects div
            pauseMS(PAUSE_MS);

            WebElement attributeDescField = findElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::input[1]")).get(0);
            sendKeysSlowly(attributeDescField,  "history");
            attributeDescField.sendKeys(Keys.DOWN);

            waitForThenClick("span","Band [Band] -> history [history]");
            scrollToBottom();
            waitForThenClick("button","Done");
            waitForNoificationToClose();

            // Add a message to the the new min constraint
            waitForThenDoBackofficeSearch(id);
            waitForThenClick("span", id);// 6.2 expects div
            waitForTagXWithAttributeYWithValueZThenClick("button","class","yw-expandCollapse z-button");
            pauseMS(PAUSE_MS);

            WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
            sendKeysSlowly(errorMessageField, "No Lorem Ipsum"+Keys.RETURN);
            waitForThenClick("button","Save");
            waitForNoificationToClose();
        }


        public static void selectConstraintsPage() {
            waitForThenClickMenuItem("System");
            waitForThenClickMenuItem("Validation");
            waitForThenClickMenuItem("Constraints");
        }

        public static void deleteExistingMinConstraint(String id) {
            try{
                waitForThenClick("span", id);// 6.2 expects div
                pauseMS(PAUSE_MS);

                List<WebElement> bins = findElements(By.xpath("//img[contains(@src,'/backoffice/widgetClasspathResource/widgets/actions/deleteAction/icons/icon_action_delete_default.png')]"));
                if (bins.size()==2)
                    scrollToThenClick(bins.get(1));
                else
                    scrollToThenClick(bins.get(0));

                waitForThenClickButtonWithText("Yes");
                waitForNoificationToClose();
            }
            catch(Exception e){
                LOG.info(e.getMessage());
            }
        }

        public static boolean waitForConnectionToOpen(String url, int waitMS) throws Exception{
            try{
                URL obj = new URL(url);
                HttpURLConnection conn;
                if (url.contains("https"))
                    conn = (HttpsURLConnection) obj.openConnection();
                else
                    conn = (HttpURLConnection) obj.openConnection();
                conn.setConnectTimeout(waitMS); // start-up can take some time
                conn.setReadTimeout(waitMS);
                conn.setRequestMethod("GET");
                // Read response
                conn.getResponseCode();
                return true;
            }
            catch(Exception e){
                return false;
            }
        }

        public static void closeBrowser(){
            if (peakDriver() != null){
                for (StackTraceElement s : Thread.currentThread().getStackTrace())
                    System.out.println(s.getMethodName());
                getDriver().close();
                getDriver().quit();
                setDriver(null);
            }
        }

    }   // Gets replaced by CreateHybris123Pages

    @Before
    public void allowHttps(){
        HttpsHelper.allowHttps();
        VersionHelper.getVersion();
    }

    @After
    public void closeSelenium(){
        try {
            closeBrowser();
        } catch (Exception e) {
            LOG.info("Exception thrown in closeSelenium::closeBrowser"+e);
        }
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testUnzippedOk")
    public void testUnzippedOk() throws Exception {
        assertTrue("The folder structure should be as shown in this method",
                FileHelper.fileExists("../../HYBRISCOMM6*.zip")  &&
                        FileHelper.fileExists("../../HYBRISCOMM6*/README") &&
                        FileHelper.fileExists("../../HYBRISCOMM6*/hybris123/src/main/java/com/hybris/hybris123/runtime/tests/Hybris123Tests.java")
        );
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testAcceleratorQuickDiveIsOk")
    public void testAcceleratorQuickDiveIsOk() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/yb2bacceleratorstorefront/?site=powertools&clear=true");
        assertTrue( getTitle().contains("Powertools Site") );
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testExtensionCreatedOk")
    public void testExtensionCreatedOk() {
        assertTrue("New Jalo files are not there",
                FileHelper.directoryExists("../hybris/bin/custom/concerttours/src/concerttours/jalo"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testCodeGeneratedOk")
    public void testCodeGeneratedOk() throws IOException {
        // If you have correctly added an extension there should be some new
        // folders and files
        assertTrue("You should have included concerttours in localextensions.xml",
                FileHelper.fileExistsAndContains("../hybris/config/localextensions.xml", "concerttours"));
        assertTrue("Running ant should have generated some sources for concerttours",
                FileHelper.directoryExists("../hybris/bin/custom/concerttours/gensrc"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testExtensionModelOk")
    public void testExtensionModelOk() throws ClassNotFoundException, IOException {
        assertTrue( "ProductModel has been extended to support Hashtag and Band",
                FileHelper.fileContains( "../hybris/bin/platform/bootstrap/gensrc/de/hybris/platform/core/model/product/ProductModel.java",
                        "getHashtag", "getBand",
                        "setHashtag", "setBand") );

        assertTrue( "A new BandModel supports Code, Name, History, Tours",
                FileHelper.fileContains( "../hybris/bin/platform/bootstrap/gensrc/concerttours/model/BandModel.java",
                        "getName","getHistory","getCode", "getTours",
                        "setName","setHistory","setCode", "setTours") );

        assertTrue( "A new ConcertModel extends VariantProductModel and supports Venue and Date",
                FileHelper.fileContains( "../hybris/bin/platform/bootstrap/gensrc/concerttours/model/ConcertModel.java",
                        "ConcertModel extends VariantProductModel",
                        "getVenue","getDate",
                        "setVenue","setDate") );

        assertTrue( "The new GeneratedBand extends GenericItem and supports Code, Name, History, Tours",
                FileHelper.fileContains( "../hybris/bin/custom/concerttours/gensrc/concerttours/jalo/GeneratedBand.java",
                        "GeneratedBand extends GenericItem",
                        "getName","getHistory","getCode", "getTours",
                        "setName","setHistory","setCode", "setTours") );

        assertTrue( "The new GeneratedConcert extends VariantProduct and supports Venue, Date",
                FileHelper.fileContains( "../hybris/bin/custom/concerttours/gensrc/concerttours/jalo/GeneratedConcert.java",
                        "GeneratedConcert extends VariantProduct",
                        "getVenue","getDate",
                        "setVenue","setDate") );
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testDatabaseSetup")
    public void testDatabaseSetup() throws Exception {
        HsqlDBHelper hsqldb = new HsqlDBHelper();
        // Note test will fail if the suite is running on this DB at the same time.
        try {
            String res = hsqldb.select("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS WHERE TABLE_NAME NOT LIKE 'SYSTEM_%'");
            assertTrue("Could not find the table BANDS",  res.contains("BANDS")  );
        } catch (Exception e) {
            fail("testDatabaseSetup", "HsqlDBTest failed: " + e.getMessage());
        } finally {
            hsqldb.shutdown();
        }
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_gitRepoOk")
    public void gitRepoOk() {
        CommandLineHelper shellCommands = new CommandLineHelper();
        String output = shellCommands.runCmd("git --git-dir .git log");
        assertTrue("Git Repo has not been set up correctly", output.contains("Set Up a Git Repository"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testServiceLayerClassesExist")
    public void testServiceLayerClassesExist() throws IOException {
        // If you have correctly added an extension there should be some new folders and files
        assertTrue("You should have added concerttours.daos.BandDAO.java", FileHelper.fileExistsAndContains(
                "../hybris/bin/custom/concerttours/src/concerttours/daos/BandDAO.java", "public interface BandDAO"));
        assertTrue("You should have added concerttours.daos.impl.DefaultBandDAO.java", FileHelper.fileExistsAndContains(
                "../hybris/bin/custom/concerttours/src/concerttours/daos/impl/DefaultBandDAO.java", "public class DefaultBandDAO implements BandDAO"));
        assertTrue("You should have modified concerttours-spring.xml", FileHelper.fileExistsAndContains(
                "../hybris/bin/custom/concerttours/resources/concerttours-spring.xml", "<context:component-scan base-package=\"concerttours\"/>"));
        assertTrue("You should have added concerttours.service.impl.DefaultBandService.java", FileHelper.fileExistsAndContains(
                "../hybris/bin/custom/concerttours/src/concerttours/service/impl/DefaultBandService.java", "public class DefaultBandService implements BandService"));
        assertTrue("You should have added concerttours.service.BandService.java", FileHelper.fileExistsAndContains(
                "../hybris/bin/custom/concerttours/src/concerttours/service/BandService.java", "public interface BandService"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testBackOffice")
    public void testBackOffice() throws Exception {
        assertTrue( loginToBackOffice() );
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testBackofficeProductListingContainsTheBands")
    public void testBackofficeProductListingContainsTheBands() {
        loginToBackOffice();
        waitForThenClickMenuItem("Catalog");
        waitForThenClickMenuItem("Products");
        assertTrue(waitForText("Grand Tour - Boston"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testValidationConstraintViaItemsXml")
    public void testValidationConstraintViaItemsXml() {
        loginToBackOffice();
        waitForThenClickMenuItem("System");
        waitForThenClickMenuItem("Types");
        waitForThenDoBackofficeSearch("Band");
        waitForThenClick("span","Band");
        waitForImageWithTitleThenClick("Search by type");
        waitForThenDoBackofficeSearch("");
        waitForThenClick("span","The Quiet");

        waitForThenUpdateInputField("The Quiet", "The Choir");
        assertTrue( waitForThenClick("button","Save") );
    }

    @Test
    public void testCreateValidationConstraintViaBackoffice() {
        loginToBackOffice();
        selectConstraintsPage();
        deleteExistingMinConstraint("NewConstraint");
        addNewMinConstraint("NewConstraint");
        reloadConstraints();
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testValidationConstraintViaBackoffice")
    public void testValidationConstraintViaBackoffice() {
        loginToBackOffice();
        selectConstraintsPage();
        tryToViolateTheNewConstraint();
        assertTrue( waitFor("span","Album sales must be > 0. Do pay attention."));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testValidationCustomConstraint")
    public void testValidationCustomConstraint() {
        loginToBackOffice();
        selectConstraintsPage();
        deleteExistingMinConstraint("NotIpsum");
        addNewCustomConstraint("NotIpsum");
        reloadConstraints();
        tryToViolateTheNewCustomConstraint();
        assertTrue( waitFor("span","No Lorem Ipsum"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testPropertiesFiles")
    public void testPropertiesFiles() {
        assertTrue(
                checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"DefaultBandFacadeIntegrationWithPropertiesTest\" package=\"concerttours.facades.impl\" tests=\"1\"(.*)") );
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testValidationConstraintAfterImpex")
    public void testValidationConstraintAfterImpex() {
        loginToBackOffice();
        modifyABandToHaveNegativeAlbumSales();
        waitFor("div","You have 1 Validation Errors");
        assertTrue( waitFor("span","Album sales must not be negative"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testSuiteIsOnline")
    public void testSuiteIsOnline() {
        assertTrue( canLoginToHybrisCommerce());
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testDynamicAttributeView")
    public void testDynamicAttributeView() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/concerttours/bands/A001");
        waitFor("a","The Grand Little x Tour");
        navigateTo("https://localhost:9002/concerttours/tours/201701");
        waitFor("th","Days Until");
        assertTrue( waitFor("td","0"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testBandImages")
    public void testBandImages() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/concerttours/bands");
        waitForValidImage();
        navigateTo("https://localhost:9002/concerttours/bands/A006");
        assertTrue( waitForValidImage());
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_loginAndCheckForConcertToursExtension")
    public void loginAndCheckForConcertToursExtension()  {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/platform/extensions") ;
        assertTrue( waitForExtensionListing("concerttours"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testNewsEvents")
    public void testNewsEvents()  {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/platform/init");
        waitForThenClickButtonWithText("Initialize");
        waitForThenClickOkInAlertWindow();
        waitForInitToComplete();
        closeBrowser();

        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/console/flexsearch");
        waitForFlexQueryFieldThenSubmit("SELECT {headline} FROM {News}");
        assertTrue( waitFor("td","New band, Banned"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_simulateInitialization")
    public void simulateInitialization() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/platform/init");
        waitForThenClickButtonWithText("Initialize");
        waitForThenClickOkInAlertWindow();
        waitForInitToComplete();
        closeBrowser();
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_simulateLoadingJobImpex")
    public void simulateLoadingJobImpex() throws Exception {
        canLoginToHybrisCommerce();
        String impex = FileHelper.getContents("src/main/webapp/resources/concerttours/resources/script/essentialdataJobs.impex");
        navigateTo("https://localhost:9002/console/impex/import");
        submitImpexScript(impex);
        waitFor("div","Import finished successfully");
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testSendingNewsMails")
    public void testSendingNewsMails() throws Exception {
        long timeSinceLastMailWasSentMS = LogHelper.getMSSinceLastNewsMailsLogged();
        assertTrue("A log of the last mail sent should have been timestamped recently "+timeSinceLastMailWasSentMS,
                timeSinceLastMailWasSentMS < 5*60*1000);
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testHookImpex")
    public void testHookImpex() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/platform/init");
        waitForThenClickButtonWithText("Initialize");
        waitForThenClickOkInAlertWindow();
        waitForInitToComplete();
        long timeSinceHookLogsFound = LogHelper.getMSSinceThisWasLogged("Custom project data loading for the Concerttours extension completed");
        assertTrue("Did not find the expected logs "+ timeSinceHookLogsFound,
                timeSinceHookLogsFound < 10000);
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testHookAndCoC")
    public void testHookAndCoC() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/platform/init");
        waitForThenClickButtonWithText("Initialize");
        waitForThenClickOkInAlertWindow();
        waitForInitToComplete();
        long timeSinceHookLogsFound = LogHelper.getMSSinceThisWasLogged("importing resource : /impex/projectdata-musictypes.impex");
        assertTrue("Did not find the expected logs",
                timeSinceHookLogsFound < 10000);
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_simulateManualImpex")
    public void simulateManualImpex() throws Exception {
        String impex = FileHelper.getContents("src/main/webapp/resources/impex/essentialdata-bands.impex");
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/console/impex/import");
        submitImpexScript(impex);
        waitFor("div","Import finished successfully");
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testManualImpex")
    public void testManualImpex() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/console/flexsearch");
        waitForFlexQueryFieldThenSubmit("SELECT {pk}, {code}, {history} FROM {Band}");
        assertTrue( waitFor("td","A cappella singing group based in Munich"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testCoCImpex")
    public void testCoCImpex() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/platform/init");
        waitForThenClickButtonWithText("Initialize");
        waitForThenClickOkInAlertWindow();
        waitForInitToComplete();
        closeBrowser();

        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/console/flexsearch");
        waitForFlexQueryFieldThenSubmit("SELECT {pk}, {code}, {history} FROM {Band}");
        assertTrue( waitFor("td","A cappella singing group based in Munich"));
    }

    // See  https://wiki.hybris.com/display/release5/hybris+Testweb+Frontend+-+End+User+Guide
    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testServiceLayerIntegrationTest")
    public void testServiceLayerIntegrationTest() throws Exception {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"DefaultBandDAOIntegrationTest\" package=\"concerttours.daos.impl\" tests=\"3\"(.*)" ) &&
                checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"DefaultBandServiceIntegrationTest\" package=\"concerttours.service.impl\" tests=\"3\"(.*)" ));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testCustomConstraintIntegrationTest")
    public void testCustomConstraintIntegrationTest() {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"NotLoremIpsumConstraintTest\" package=\"concerttours.constraints\" tests=\"1\"(.*)" ));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testLocalizedServiceLayerTest")
    public void testLocalizedServiceLayerTest() {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"2\" (.*) name=\"DefaultBandFacadeUnitTest\" package=\"concerttours.facades.impl\" tests=\"2\" (.*)") &&
                checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"DefaultBandServiceUnitTest\" package=\"concerttours.service.impl\" tests=\"2\"(.*)"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testFacadeLayerOk")
    public void testFacadeLayerOk()  {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"DefaultBandFacadeIntegrationTest\" package=\"concerttours.facades.impl\" tests=\"3\"(.*)" ) );
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testIsOnline")
    public void testIsOnline() throws Exception {
        //	Careful = this opens a second selenium..	canLoginToHybrisCommerce();
        int waitMS= 2000;
        assertTrue( waitForConnectionToOpen("https://localhost:9002/login.jsp", waitMS) );
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testWebAppComponent")
    public void testWebAppComponent() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/concerttours/bands");
        waitFor("a","The Quiet");
        navigateTo("https://localhost:9002/concerttours/bands/A007");
        assertTrue( waitFor("p","English choral society specialising in beautifully arranged, soothing melodies and songs"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testServiceLayerUnitTest")
    public void testServiceLayerUnitTest()  {
        assertTrue( checkTestSuiteXMLMatches("(.*)<testsuite errors=\"0\" failures=\"0\" (.*) name=\"DefaultBandServiceUnitTest\" package=\"concerttours.service.impl\"(.*)" ));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testDynamicAttributeIntegrationTest")
    public void testDynamicAttributeIntegrationTest()  {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"ConcertDaysUntilAttributeHandlerIntegrationTest\" package=\"concerttours.attributehandlers\" tests=\"3\" (.*)" ));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testRESTCalls")
    public void testRESTCalls() throws Exception {
        String response = callCurl("curl", "-k", "-X", "GET", "https://localhost:9002/ws410/rest/bands/yRock", "-H", "authorization: Basic YWRtaW46bmltZGE='");
        assertTrue( "Response should include the bands as XML", response.contains("<band name=\"yRock\""));

        response = callCurl("curl", "-k", "-X", "GET", "https://localhost:9002/ws410/rest/bands/yRock", "-H", "accept: application/json", "-H", "authorization: Basic YWRtaW46bmltZGE='");
        assertTrue( "Response should include band details as JSON", response.contains("@name\":\"yRock\",\"@pk\":\"8796093085244\""));
        response = callCurl("curl", "-k", "-X", "PUT", "https://localhost:9002/ws410/rest/bands/yRock", "-H", "authorization: Basic YWRtaW46bmltZGE='", "-H", "content-type: text/xml", "-d", "<band code='A001'><history>The VERY biggest, baddest rock band in software vendor history</history></band>");
        assertTrue("Response should be empty", response.length()==0);

        response = callCurl("curl", "-k", "-X", "GET", "https://localhost:9002/ws410/rest/bands/yRock", "-H", "accept: application/json", "-H", "authorization: Basic YWRtaW46bmltZGE='");
        assertTrue( "Response should include band details as JSON", response.contains("\"history\":\"The VERY biggest, baddest rock band in software vendor history\""));

        response = callCurl("curl", "-k", "-X", "PUT", "https://localhost:9002/ws410/rest/cronjobs/sendNewsCronJob?cmd=StartCronJobCommand", "-H", "authorization: Basic YWRtaW46bmltZGE='", "-H", "content-type: text/xml", "-d", "<cronjob code='sendNewsCronJob'/>"  );
        assertTrue("Response should be empty", response.length()==0);
    }


    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testDynamicAttributeUnitTest")
    public void testDynamicAttributeUnitTest()  {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"ConcertDaysUntilAttributeHandlerUnitTest\" package=\"concerttours.attributehandlers\" tests=\"3\" (.*)" ));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testEventInterceptorIntegrationTest")
    public void testEventInterceptorIntegrationTest() {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"BandAlbumSalesEventListenerIntegrationTest\" package=\"concerttours.events\" tests=\"2\" (.*)" ));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testAsyncEventInterceptorIntegrationTest")
    public void testAsyncEventInterceptorIntegrationTest()  {
        assertTrue(
                checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"BandAlbumSalesEventListenerIntegrationTest\" package=\"concerttours.events\" tests=\"2\" (.*)" ) &&
                        checkTestSuiteXMLMatches("(.*)testcase classname=\"concerttours.events.BandAlbumSalesEventListenerIntegrationTest\" name=\"testEventSendingAsync\"(.*)"));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testSendNewsJobIntegrationTest")
    public void testSendNewsJobIntegrationTest() {
        assertTrue( checkTestSuiteXMLMatches("(.*)testsuite errors=\"0\" failures=\"0\" (.*) name=\"SendNewsJobIntegrationTest\" package=\"concerttours.jobs\" tests=\"2\" (.*)" ));
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_simulateGroovyScript")
    public void simulateGroovyScript() throws Exception {
        String groovyScript = FileHelper.getContents("src/main/webapp/resources/concerttours/resources/script/groovyjob.script");
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/console/scripting");
        waitForGroovyWindowThenSubmitScript(groovyScript);
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testGroovyScript")
    public void testGroovyScript() throws Exception {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/console/flexsearch");
        waitForFlexQueryFieldThenSubmit("SELECT {p.pk}, {p.code}, {p.name}, {q.code} FROM {Concert AS p}, {ArticleApprovalStatus AS q} WHERE {p.approvalstatus} = {q.pk}");
        assertTrue( waitForText("check") );
    }

    @Test
    public void working() throws Exception{
        try {
            for (int i=0;i<10;i++){
                testBackofficeLocalization();
                closeBrowser();
            }
        } catch (Exception e) {
            Thread.sleep(100000);
        }
    }

    @Test
    @Snippet("com.hybris.hybris123.Hybris123Tests_testBackofficeLocalization")
    public void testBackofficeLocalization() {
        canLoginToHybrisCommerce();
        navigateTo("https://localhost:9002/platform/init");
        waitForThenClickButtonWithText("Initialize");
        waitForThenClickOkInAlertWindow();
        waitForInitToComplete();
        closeBrowser();

        loginToBackOffice("Deutsch");
        waitForThenClickMenuItem("System");
        waitForThenClickMenuItem("Typen");
        waitForThenDoBackofficeSearch("Concert");
        waitForThenAndClickSpan("Concert");
        waitForThenAndClickSpan("Eigenschaften");
        waitForThenClickDotsBySpan("daysUntil");
        waitForThenAndClickSpan("Details bearbeiten", "Edit Details");
        assertTrue( waitForValue("input", "Tage bis es stattfindet") );
    }

    private static void assertTrue(boolean condition) {
        assertTrue(null, condition);
    }

    private static void assertTrue(String message, boolean condition) {
        String methodName = getMethodName();
        try{
            org.junit.Assert.assertTrue(message, condition);
            updateTestStatus( "com.hybris.hybris123.Hybris123Tests_"+methodName, "passed");
        }
        catch(Error e){
            updateTestStatus( "com.hybris.hybris123.Hybris123Tests_"+methodName, "failed");
            org.junit.Assert.fail(message);
        }
        catch(Exception e){
            updateTestStatus( "com.hybris.hybris123.Hybris123Tests_"+methodName, "failed");
            org.junit.Assert.fail(message);
        }
    }

    public static void fail(String callingMethod, String message) {
        LOG.debug("In fail "+callingMethod+" "+message);
        updateTestStatus( "com.hybris.hybris123.Hybris123Tests_"+callingMethod, "failed");
        org.junit.Assert.fail(message);
        try {
            if (WAITONFAIL)
                Thread.sleep(500000);
        } catch (InterruptedException e) {
            LOG.error( e.getMessage() );
        }
    }

    public static void fail(String callingMethod) {
        fail(callingMethod, null);
    }

    private static void updateTestStatus(String name, String status){
        try {
            waitForConnectionToOpen("http://localhost:8080/hybris123/tdd?test=updatelog&testName="+name+"&testStatus="+status, 1000);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    public static boolean fileExists(String s){
        return FileHelper.fileExists(s);
    }

    public static String runCmd(String s){
        return CommandLineHelper.runCmd(s);
    }
}

/*
CommandLineHelper.java */



class CommandLineHelper {

    public static String runCmd(String cmd){
        String output = "";
        try {
            if (!cmd.equals("git --git-dir .git log") &&
                    !cmd.equals("mvn.cmd --version") &&
                    !cmd.equals("mvn --version") &&
                    !cmd.equals("git --version") )
                throw new Exception ("Command not the one expected");

            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(  new InputStreamReader(p.getInputStream()));
            String s ="";
            int maxlines = 1000;
            while ((s = br.readLine()) != null && maxlines-- > 0){
                output = output.concat(s);
            }
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            return "EXCEPTION: "+e;
        }
        return output;
    }
}


/*
FileHelper.java */




class FileHelper {

    private static final Logger LOG = LoggerFactory.getLogger(Hybris123Tests.class);

    private static boolean STATICVERSION = true;

    public  static String getPath(String file) {
        String s = new File(file).getAbsolutePath();
        if (s.lastIndexOf('/')!=-1)
            s = s.substring(0, s.lastIndexOf('/'));
        if (s.lastIndexOf('\\')!=-1)
            s = s.substring(0, s.lastIndexOf('\\'));
        return s;
    }
    public static String getContents(String file) throws IOException {
        if (STATICVERSION){
            if (file.equals("src/main/webapp/resources/impex/essentialdata-bands.impex"))
                return InPlaceContents.essentialdatabandsimpex;
            if (file.equals("src/main/webapp/resources/concerttours/resources/script/essentialdataJobs.impex"))
                return InPlaceContents.essentialdatajobsimpex;
            if (file.equals("src/main/webapp/resources/concerttours/resources/script/groovyjob.script"))
                return InPlaceContents.groovyjobscript.replaceAll("'", "\"");
        }
        String impex = new String(Files.readAllBytes(new File(file).toPath()), ("UTF-8"));
        impex = impex.replace("\r", "");
        return impex;
    }

    public static String getContentsExcludingPackageAndImports(String file) throws IOException {
        String s = Files.readAllLines(
                Paths.get(new File(file).toURI())).
                stream().
                filter(l -> l.indexOf("package") != 0 && l.indexOf("import") != 0 && l.indexOf("@ManagedBean") != 0).
                reduce("", (x, y) -> x.concat("\n").concat(y));



        // Remove copyright lines
        s = s.replaceAll(" \\* © 2017 SAP SE or an SAP affiliate company.(.*)\n", new File(file).getName());
        s = s.replaceAll(" \\* All rights reserved.(.*)\n",  "");
        s = s.replaceAll(" \\* Please see http://www.sap.com/corporate-en/legal/copyright/index.epx for additional trademark information and(.*)\n",  "");
        s = s.replaceAll(" \\* notices.(.*)\n",  "");
        return s;
    }


    public static String getContents(URI fileURI) throws IOException {
        return Files.readAllLines(Paths.get(fileURI)).stream().reduce("", (x, y) -> x.concat("\n").concat(y));
    }


    public static boolean directoryExists(String path) {
        LOG.debug("CHECKING IF THIS Directory EXISTS {} {}", new File(path).getPath(), new File(path).exists());
        return new File(path).exists();
    }

    public static boolean fileExists(String f) {
        return fileExistsRecursive(Paths.get("."), f);
    }

    public static File lastModifiedLogFile() {
        String logPath = "./../hybris/log/tomcat";
        File logDir = new File(logPath);
        LOG.debug("lastModifiedLogFile logDir {}", logDir.getAbsolutePath());
        File[] files = logDir.listFiles(f -> f.isFile() && f.getName().startsWith("console"));
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        LOG.debug("Reading log file {}", choice);
        return choice;
    }

    public static boolean fileExistsRecursive(Path path, String f) {

        if (f.contains("../")) {
            String right = f.substring(f.indexOf("../") + 3);
            return fileExistsRecursive(path.resolve("../"), right);
        } else if (f.contains("/")) {
            String left = f.substring(0, f.indexOf('/'));
            String right = f.substring(f.indexOf('/') + 1);
            FileFilter fileFilter = new WildcardFileFilter(left);
            File[] files = path.toFile().listFiles(fileFilter);
            boolean candidate = false;
            for (File file : files) {
                if (file.isDirectory())
                    candidate = candidate || fileExistsRecursive(file.toPath(), right);
            }
            return candidate;
        } else {
            FileFilter fileFilter = new WildcardFileFilter(f);
            File[] files = path.toFile().listFiles(fileFilter);
            List<File> list = new ArrayList<>(Arrays.asList(files));
            return !list.isEmpty();
        }
    }

    public static boolean fileExistsAndContains(String f, String s) {
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(f)));
        } catch (IOException e) {
            return false;
        }
        return content.contains(s);
    }


    public static boolean fileContains(String file, String... setOfStrings) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(file)));
        for (String s : setOfStrings) {
            if (!content.contains(s))
                return false;
        }
        return true;
    }

    public static String prepareForInsiderQuotes(String s) {
        return s.trim().replaceAll("\"","'").replaceAll("\n", "\\\\n");
    }


    public static boolean dirIsNotEmpty(String dir){
        return new File(dir).list().length > 0;

    }

    public static boolean dirIsEmpty(String dir){
        return new File(dir).list().length == 0;

    }

}

/*
HsqlDBHelper.java */



/**
 * A helper to allow users to directly invoke HSQL queries from hybris123
 */
class HsqlDBHelper {
    private Connection conn;
    private final String HSQLDB = "jdbc:hsqldb:file:./../hybris/data/hsqldb/mydb";
    private static final Logger LOG = LoggerFactory.getLogger(HsqlDBHelper.class);

    public HsqlDBHelper() throws ClassNotFoundException, SQLException {
        Class.forName("org.hsqldb.jdbcDriver");        // Loads the HSQL Database Engine JDBC driver
        // !!Note that leaving your default password as the empty string in production would be a major security risk!!
        conn = DriverManager.getConnection(HSQLDB,  "sa",   "");
    }

    public void shutdown( ) throws SQLException {
        Statement st = conn.createStatement();
        st.execute("SHUTDOWN");
        conn.close();
    }

    public synchronized String select( String expression) throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        st = conn.createStatement();
        rs = st.executeQuery(expression);
        String res = dump(rs);
        st.close();
        return res;
    }

    public String dump(ResultSet rs) throws SQLException {
        ResultSetMetaData meta   = rs.getMetaData();
        int               colmax = meta.getColumnCount();
        int               i;
        String            o;
        String result = "";
        while(rs.next()) {
            for (i = 1; i <= colmax; i++) {
                if (i>1)
                    result = result.concat(" ");
                o = (rs.getObject(i ) == null) ? "NULL": rs.getObject(i ).toString();
                result = result.concat(o);
            }
            result = result.concat("\n");
        }
        return result;
    }
}

/*
HttpsHelper.java */





class HttpsHelper {

    private static final Logger LOG = LoggerFactory.getLogger(HttpsHelper.class);

    public static void allowHttps() {
        try{
            // Create a context that doesn't check certificates.
            SSLContext ssl_ctx = SSLContext.getInstance("TLS");
            TrustManager[] trust_mgr = get_trust_mgr();
            ssl_ctx.init(null, // key manager
                    trust_mgr, // trust manager
                    new SecureRandom()); // random number generator
            HttpsURLConnection.setDefaultSSLSocketFactory(ssl_ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            CookieHandler.setDefault(new CookieManager(null, null));
        }catch(Exception e){
            LOG.error(e.getMessage());
        }

    }

    public static TrustManager[] get_trust_mgr() {
        TrustManager[] certs = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String t) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String t) {
            }
        } };
        return certs;
    }
}

/*
LogHelper.java */




class LogHelper {
    private static final Logger LOG = LoggerFactory.getLogger(LogHelper.class);

    public static long getMSSinceThisWasLogged(String entry) throws Exception {
        // At present searches the log files - could to better with Mbeans?
        // Search for "Sending news mails"
        // INFO | jvm 1 | main | 2017/05/22 12:15:32.809 | [32mINFO
        // [sendNewsCronJob::de.hybris.platform.servicelayer.internal.jalo.ServicelayerJob]
        // (sendNewsCronJob) [SendNewsJob] Sending news mails
        // Try a few times as it can take time for the log to update

        int nTries = 5;
        long msSinceLastLogLine = -1;
        for (int i = 0; i < nTries; i++){
            File logFile = FileHelper.lastModifiedLogFile();
            try (Stream<String> stream = Files.lines(logFile.toPath())) {
                msSinceLastLogLine = stream.filter(line -> line.contains(entry))
                        .map(LogHelper::getMSSinceGivenDate).reduce((first, second) -> second).get();

                LOG.debug("In getMSSinceThisWasLogged {} ms: {}", entry, msSinceLastLogLine);
                if ( msSinceLastLogLine != -1)
                    return msSinceLastLogLine ;
            } catch (IOException e) {
                LOG.debug("In getMSSinceThisWasLogged {} IOExn Thrown: ", entry);
            } catch (Exception e) {
                LOG.debug("In getMSSinceThisWasLogged Exn thrown ");
            }
            Thread.sleep(5000);
        }

        return msSinceLastLogLine;
    }
    public static long getMSSinceLastNewsMailsLogged() throws Exception {
        return getMSSinceThisWasLogged("Sending news mails");
    }

    private static long getMSSinceGivenDate(String line) {
        try {
            String time = StringUtils.tokenizeToStringArray(line, "|")[3].trim();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.S");
            Date d1 = formatter.parse(time);
            return new Date().getTime() - d1.getTime();
        } catch (Exception e) {
            return -1;
        }
    }
}


/*
Snippet.java */

@interface Snippet {
    public String value() default "";
}





class VersionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(VersionHelper.class);

    private static String version = null;

    public static boolean is67() {
        return version.startsWith("6.7");
    }
    public static boolean is66() {
        return version.startsWith("6.6");
    }
    public static boolean is65() {
        return version.startsWith("6.5");
    }
    public static boolean is64() {
        return version.startsWith("6.4");
    }
    public static boolean is63() {
        return version.startsWith("6.2");
    }
    public static boolean is62() {
        return version.startsWith("6.2");
    }
    public static boolean is61() {
        return version.startsWith("6.1");
    }
    public static boolean is60() {
        return version.startsWith("6.0");
    }

    public static String getVersion() {
        if(version!=null)
            return version;

        String buildNumberPath = "./../hybris/bin/platform/build.number";
        try (Stream<String> stream = Files.lines(Paths.get(buildNumberPath))) {
            version = stream.filter(s->s.contains("version=")).findFirst().get().split("=")[1];
        } catch (IOException e) {
            LOG.error("Version not found");
        }
        return version;
    }

}



interface InPlaceContents {
    // Gets relaced by CreateHybris123Pages
    public String essentialdatabandsimpex ="# Hybris123File \n# ImpEx for Importing Bands into Little Concert Tours Store\n \nINSERT_UPDATE Band;code[unique=true];name;albumSales;history\n;A001;yRock;1000000;Occasional tribute rock band comprising senior managers from a leading commerce software vendor\n;A006;yBand;;Dutch tribute rock band formed in 2013 playing classic rock tunes from the sixties, seventies and eighties\n;A003;yJazz;7;Experimental Jazz group from London playing many musical notes together in unexpected combinations and sequences\n;A004;Banned;427;Rejuvenated Polish boy band from the 1990s - this genre of pop music at its most dubious best\n;A002;Sirken;2000;A cappella singing group based in Munich; an uplifting blend of traditional and contemporaneous songs\n;A005;The Choir;49000;Enthusiastic, noisy gospel choir singing traditional gospel songs from the deep south\n;A007;The Quiet;1200;English choral society specialising in beautifully arranged, soothing melodies and songs";
    // Gets relaced by CreateHybris123Pages
    public String essentialdatajobsimpex = "# Hybris123SnippetStart essentialdataJobs\n# Define the cron job and the job that it wraps\nINSERT_UPDATE CronJob; code[unique=true];job(code);singleExecutable;sessionLanguage(isocode)\n;sendNewsCronJob;sendNewsJob;false;de\n \n# Define the trigger that periodically invokes the cron job\nINSERT_UPDATE Trigger;cronjob(code)[unique=true];cronExpression\n#% afterEach: impex.getLastImportedItem().setActivationTime(new Date());\n; sendNewsCronJob; 0/10 * * * * ?\n\n# Hybris123SnippetEnd";
    // Gets relaced by CreateHybris123Pages
    public String groovyjobscript = "// Hybris123SnippetStart groovyjob\nimport de.hybris.platform.cronjob.enums.*\nimport de.hybris.platform.servicelayer.cronjob.PerformResult\nimport de.hybris.platform.servicelayer.search.*\nimport de.hybris.platform.servicelayer.model.*\nimport de.hybris.platform.catalog.enums.ArticleApprovalStatus \nimport concerttours.model.ConcertModel\n  \nsearchService = spring.getBean('flexibleSearchService')\nmodelService = spring.getBean('modelService')\nquery = new FlexibleSearchQuery('Select {pk} from {Concert}');\nsearchService.search(query).getResult().each {\n  if (it.daysUntil < 1) \n  { \n    it.approvalStatus = ArticleApprovalStatus.CHECK\n  }\n  modelService.saveAll()\n}\n\n// Hybris123SnippetEnd";
}
