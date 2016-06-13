package com.eagles13.bookdownloader;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebView;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    private TextField txtUrl;
    @FXML
    private TextField txtFileName;
    @FXML
    private Button btnCookies;
    @FXML
    private Label lblInfo;
    @FXML
    private WebView wbMain;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        wbMain.getEngine().load("http://www.google.com");

        showMessage("Please navigate to the book you want to download in the browser below, type in a title, and then click 'Download book'!", "Information");

        txtUrl.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                wbMain.getEngine().load(txtUrl.getText().startsWith("http") ?  txtUrl.getText() : "http://" +txtUrl.getText());
            }
        });


        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        CookieStore cookieJar = cookieManager.getCookieStore();
        List<HttpCookie> cookies = cookieJar.getCookies();



        txtFileName.textProperty().addListener((observable, oldValue, newValue) -> {
            btnCookies.setDisable(txtFileName.getText() == null || txtFileName.getText().isEmpty() || txtFileName.getText().contains("."));

        });

        //Hurray for nested try...catch statements!
        btnCookies.setOnAction(event -> {
            String webpageSource = (String) wbMain.getEngine().executeScript("document.documentElement.outerHTML");
            String fileName = txtFileName.getText();
            Runnable bookDownloader = () -> {

                List<String> wantedCookies = Arrays.asList(".b2cauth", "asp.net_sessionid", "shib_entity", "shib_personalid", "shib_username", "shibtitleid");
                String aggregatedCookies = cookies.stream().filter(e -> wantedCookies.contains(e.getName().toLowerCase())).map(HttpCookie::toString).collect(Collectors.joining(";"));

                if (!webpageSource.contains("var pagecount")) {
                    Platform.runLater(() -> showMessage("This does not appear to be a MyiLibrary webpage.", "Error"));
                    return;
                }

                int pageCount = Integer.parseInt(webpageSource.substring(webpageSource.indexOf("var pagecount") + 13, webpageSource.indexOf("var pagecount") + 21).replaceAll("[^0-9]", ""));

                Document document = new Document();
                try {
                    PdfWriter.getInstance(document, new FileOutputStream(fileName + ".pdf"));
                    document.open();

                    Collection<Integer> elements = new LinkedList<>();
                    for (int i = 1; i <= pageCount; ++i) {
                        elements.add(i);
                    }
                    AtomicInteger downloadCounter = new AtomicInteger(0);
                    //horrible hack to speed things up
                    elements.parallelStream().forEach(i -> {
                        try {
                            downloadCounter.getAndIncrement();
                            String token = getToken(aggregatedCookies, 2);
                            downloadImage(token, i, aggregatedCookies);

                            Platform.runLater(() -> lblInfo.setText("Downloaded: " + downloadCounter.get() + " / " + pageCount + " pages"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    for (int i = 1; i <= pageCount; i++) {
                        Image img = Image.getInstance("output\\" + String.valueOf(i) + ".jpg");
                        float scale = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin()) / img.getWidth()) * 100;
                        img.scalePercent(scale);
                        document.add(img);
                    }

                    document.close();
                    Desktop.getDesktop().open(new File(fileName + ".pdf"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            };


            Thread thread1 = new Thread(bookDownloader);
            thread1.start();


        });

    }


    private void showMessage(String text, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);

        alert.showAndWait();
    }

    private String getToken(String cookies, int page) throws Exception {

        String url = "http://lib.myilibrary.com/Ajax/Open.asmx/CheckFlowWarning";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("POST");

        //add request header

        String toPost = "{'strID': '0','strPage': '" + String.valueOf(page) + "', 'strPageHeight': '1366','strPageWidth': '702', 'strC': 'false'}";

        con.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");
        con.setRequestProperty("Accept", "application/json, text/javascript, */*");
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Accept-Language", "en-gb,en-us;q=0.8,en;q=0.7");
        con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        con.setRequestProperty("cookie", cookies);
        con.setRequestProperty("Content-Length", String.valueOf(toPost.length()));
        con.setRequestProperty("Content-Type", "application/json");

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(toPost);
        wr.flush();
        wr.close();


        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        String resp = response.toString();

        String firstPart = resp.substring(0, resp.indexOf("|"));
        return resp.replace(firstPart, "").replace("|", "").replace("\"", "");
    }


    private boolean downloadImage(String auth, int page, String cookies) throws Exception {
        String url = "http://lib.myilibrary.com/Viewer/getImage_Servlet.aspx?page=1&lpage=" + String.valueOf(page) + "&width=1366&height=702&searchphrase=&quality=82&codec=jpg&sid=" + auth;

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestProperty("cookie", cookies);
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("Accept-Language", "en-gb,en-us;q=0.8,en;q=0.7");
        con.setRequestProperty("Accept", "image/webp,image/*,*/*;q=0.8");
        // optional default is GET
        con.setRequestMethod("GET");

        InputStream in = con.getInputStream();
        File dir = new File("output");
        if (dir.exists() || (!dir.exists() && dir.mkdir())) {
            FileOutputStream out = new FileOutputStream(dir.getAbsolutePath() + "\\" + String.valueOf(page) + ".jpg");
            int c;
            byte[] b = new byte[1024];
            while ((c = in.read(b)) != -1)
                out.write(b, 0, c);
            return true;
        }

        return false;

    }

}
