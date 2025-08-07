import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);
        System.out.println("Welcome to ☁️ The Java Weather App ☀️");
        sleep(2000);

        System.out.println("Enter a City and Receive Real-Time Weather Data. ");
        sleep(2000);

        System.out.println("Fetching Real-Time Weather Data...");
        sleep(2000);

        System.out.println("Please Wait...");
        sleep(2000);

        System.out.println("Ready to Begin!");

        try {
            String city;
            do {
                System.out.println("+---------------------------------------+");
                System.out.print("Please Enter a City or Press 'x' to Exit: ");
                city = userInput.nextLine();
                if (city.equalsIgnoreCase("x")) {
                    System.out.println();
                    System.out.println("Goodbye!");
                    break;
                }

                JSONObject cityLocationData = getLocationData(city);

                if (cityLocationData == null) {
                    System.out.println("City not found. Please try again.");
                    continue;
                }

                double latitude = (double) cityLocationData.get("latitude");
                double longitude = (double) cityLocationData.get("longitude");


                // Get timezone string from API result
                String timezone = (String) cityLocationData.get("timezone");
                ZoneId cityZone;
                if (timezone != null && !timezone.isEmpty()) {
                    try {
                        cityZone = ZoneId.of(timezone);
                    } catch (DateTimeException e) {
                        System.out.println("Invalid timezone from API: " + timezone + ", using system default.");
                        cityZone = ZoneId.systemDefault();
                    }
                } else {
                    System.out.println("Timezone missing from API, using system default.");
                    cityZone = ZoneId.systemDefault();
                }

                System.out.print("Showing weather for: " + cityLocationData.get("name"));
                //In the API (open-meteo.com) the state is represented by "admin1"
                //admin1 = respective state based on city passed in
                if (cityLocationData.get("admin1") != null) {
                    System.out.print(", " + cityLocationData.get("admin1"));
                }
                if (cityLocationData.get("country") != null) {
                    System.out.print(", " + cityLocationData.get("country"));
                }
                System.out.println();

                displayWeatherData(latitude, longitude, cityZone);


            } while (!city.equalsIgnoreCase("x"));
        } catch (Exception e) {
            System.out.println("An unexpected error occurred. Please try again.");
            e.printStackTrace();
        } finally {
            userInput.close();
        }
    }

    private static JSONObject getLocationData(String city) {
        city = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + city + "&count=5&language=en&format=json";

        try {
            HttpURLConnection apiConnection = fetchApiResponse(url);
            if (apiConnection == null || apiConnection.getResponseCode() != 200) {
                System.out.println("Error! Could not connect to location API.");
                return null;
            }

            String jsonResponse = readApiResponse(apiConnection);
            JSONParser parser = new JSONParser();
            JSONObject resultsOfJSON_Object = (JSONObject) parser.parse(jsonResponse);
            JSONArray locationData = (JSONArray) resultsOfJSON_Object.get("results");

            if (locationData == null || locationData.isEmpty()) {
                return null;
            }if (locationData.size() == 1) {
                return (JSONObject) locationData.get(0);
            }

            System.out.println("Multiple matches found! Please select the correct location by number.");
            for (int i = 0; i < locationData.size(); i++) {
                JSONObject place = (JSONObject) locationData.get(i);
                String name = (String) place.get("name");
                String state = place.get("admin1") != null ? (String) place.get("admin1") : "N/A";
                String country = (String) place.get("country");
                System.out.printf("%d. %s, %s, %s%n", i + 1, name, state, country);
            }

            Scanner scanner = new Scanner(System.in);
            int choice;
            while (true) {
                System.out.println("+---------------------------------------+");
                System.out.print("Choose a location by number (1-" + locationData.size() + "): ");
                if (scanner.hasNextInt()) {
                    choice = scanner.nextInt();
                    if (choice >= 1 && choice <= locationData.size()) {
                        break;
                    }
                }
                System.out.println("Invalid selection.");
                scanner.nextLine(); // Clear invalid input
            }

            return (JSONObject) locationData.get(choice - 1);

        } catch(Exception e) {
            System.out.println("An unexpected error occurred. Please try again.");
            e.printStackTrace();
        }
        return null;
    }

    private static void displayWeatherData(double latitude, double longitude, ZoneId cityZone) {
        double conversionForKilometerPerHour_to_MilesPerHour = 0.621371;
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m";
            HttpURLConnection apiConnection = fetchApiResponse(url);

            if (apiConnection == null || apiConnection.getResponseCode() != 200) {
                System.out.println("Error! Could not connect to location API.");
                return;
            }

            String jsonResponse = readApiResponse(apiConnection);

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonResponse);
            JSONObject currentWeatherInJSON = (JSONObject) jsonObject.get("current");

            ZonedDateTime realLocalTime = ZonedDateTime.now(cityZone);
            String formattedRealTime = realLocalTime.format(DateTimeFormatter.ofPattern("hh:mm a"));

            System.out.println("Current time: " + formattedRealTime);

            double temperatureCelsius  = (double) currentWeatherInJSON.get("temperature_2m");
            double temperatureFahrenheit = (temperatureCelsius * 9 / 5) + 32;
            int tempFahrenheitInt = (int) Math.round(temperatureFahrenheit);
            int tempCelsiusInt = (int) Math.round(temperatureCelsius);
            System.out.printf("Current temperature: %d °F (%d °C)%n", tempFahrenheitInt, tempCelsiusInt);


            long relativeHumidity = (long) currentWeatherInJSON.get("relative_humidity_2m");
            System.out.println("Relative Humidity: " + relativeHumidity + "%");

            double windSpeedInKilometersPerHour = (double) currentWeatherInJSON.get("wind_speed_10m");
            double windSpeedInMilesPerHour = windSpeedInKilometersPerHour * conversionForKilometerPerHour_to_MilesPerHour;
            System.out.printf("Wind Speed: %.1f mph (%.1f km/h)%n", windSpeedInMilesPerHour, windSpeedInKilometersPerHour);
        } catch (Exception e) {
            System.out.println("An unexpected error occurred. Please try again.");
            e.printStackTrace();
        }
    }

    private static String readApiResponse(HttpURLConnection apiConnection) {
        try {
            StringBuilder resultingJSON_Data = new StringBuilder();

            Scanner userInput = new Scanner(apiConnection.getInputStream());

            while (userInput.hasNext()) {
                resultingJSON_Data.append(userInput.nextLine());
            }
            userInput.close();

            return resultingJSON_Data.toString();
        } catch (IOException e) {
            System.out.println("An unexpected error occurred. Please try again.");
            e.printStackTrace();
        }
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String stringAsURL) {
        try {
            URL url = new URL(stringAsURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(5000); // giving 5 seconds to establish connection
            connection.setReadTimeout(5000);   // 5 seconds to read data

            connection.setRequestMethod("GET");

            return connection;
        } catch(IOException e) {
            System.out.println("An unexpected error occurred. Please try again.");
            e.printStackTrace();
        }

        return null;
    }

     public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
