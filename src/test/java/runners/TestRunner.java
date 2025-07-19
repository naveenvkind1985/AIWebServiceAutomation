package runners;

import api.Utils.ConfigReader;
import models.StepDefinitionGenerator;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.notification.Failure;

// Test Runner to generate files or execute tests.
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",  // Path to feature files
        glue = "stepDefinition",                   // Path to step definition files
        plugin = {
                "pretty",
                "json:target/cucumber.json",
                "html:target/cucumber-reports.html",
                "junit:target/cucumber-reports/cucumber-junit.xml",
                "json:target/json-report/cucumber.json",
                "json:target/cucumber.json",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
        },
        monochrome = true,
        tags = "@Test"                             // Only execute tests tagged with "@Test"
)
public class TestRunner {
    public static void main(String[] args) {
        boolean generateFiles = true; // Change this flag to true or false to control behavior

        if (generateFiles) {
            try {
                String swaggerUrl = ConfigReader.getProperty("swaggerUrl"); // Replace with actual URL
                System.out.println("[INFO] Starting file generation...");
                System.out.println("[DEBUG] Swagger file location: " + swaggerUrl);
                StepDefinitionGenerator.processAndGenerate(swaggerUrl); // Process to generate feature files and step definitions
            } catch (Exception e) {
                System.out.println("[ERROR] Failed during file generation: " + e.getMessage());
                return; // Exit if file generation fails
            }
        }

        // Proceed executing the tests regardless of the file generation outcome
        try {
            System.out.println("[INFO] Starting test execution...");
            Result result = JUnitCore.runClasses(TestRunner.class); // Run tests

            // Process the results
            for (Failure failure : result.getFailures()) {
                System.out.println("[ERROR] Test failed: " + failure.toString());
            }

            System.out.println("[INFO] All tests executed. Number of tests failed: " + result.getFailureCount());
            if (result.wasSuccessful()) {
                System.out.println("[INFO] All tests passed.");
            } else {
                System.out.println("[INFO] Some tests failed.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed during Test Run: " + e.getMessage());
        }
    }
}