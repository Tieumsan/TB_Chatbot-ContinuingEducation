package hesge.legrand.tb.education;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.sdk.core.service.model.GenericModel;
import com.ibm.watson.natural_language_understanding.v1.model.CategoriesResult;
import com.ibm.watson.natural_language_understanding.v1.model.ConceptsResult;
import hesge.legrand.tb.chatbot.helper.Constants;
import hesge.legrand.tb.chatbot.conversation.model.NaturalLanguageUnderstandingModule;
import hesge.legrand.tb.education.model.Question;
import hesge.legrand.tb.education.model.Theme;
import io.reactivex.annotations.Experimental;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to transform a .csv file created by the course's instructors into a List of Question
 * The List of Question is then serialized to a .json file to be usable in the EducativeChatbot class.
 *
 * A Question is composed of a THEME (which must be present in education.model.Theme), a question, a typical correct answer and an optional hint
 * The typical correct answer is then being analyzed by our NaturalLanguageUnderstandingModule
 * to set the categories and concepts the user's answer must be about
 *
 * String inputFilePath : path of the file containing THEME, question, typical correct answer
 * String outputFilePath : path of the file to be written
 * List<Question> lstQuestions : List of all questions found in the given inputFilePath
 */
public class Initializer {

    private static Initializer instance;
    private String inputFilePath;
    private String outputFilePath;
    private List<Question> lstQuestions;

    public static Initializer getInstance() {
        if (instance == null) {
            instance = new Initializer();
        }
        return instance;
    } //getInstance

    /**
     * The Initializer needing to be a Singleton and having 2 filePaths to be effective,
     * we must initialize it after it has been instantiated.
     * Singleton instantiated with parameters is not really Singleton otherwise
     */
    private Initializer() {
    }

    public void initialize(String inputFilePath) throws IOException {
        this.inputFilePath = inputFilePath;
//        this.outputFilePath = outputFilePath;
        initializeQuestions();
    } //initialize

    /**
     * Serialize the List of Question created from initializeQuestions
     * and write the result in the user's specified filePath output
     * @param output : output filePath
     * @throws IOException
     */
    @Experimental
    private void serializeQuestions(String output) throws IOException {
        FileWriter jsonFileWriter = new FileWriter(output);
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
        gson.toJson(lstQuestions, jsonFileWriter);

        jsonFileWriter.flush();
        jsonFileWriter.close();
    } //serializeQuestions

    /**
     * Returns a List of questions about the specified theme
     * @param theme : Theme we want the questions to be about
     * @return : List<Question> where (foreach Question q, q.getTheme() is equals to a given theme parameter)
     */
    public List<Question> filterQuestions(Theme theme) {
        List<Question> lstFilteredQuestions = new ArrayList<>();

        for (Question question : lstQuestions) {
            if (question.getTheme().equals(theme)) {
                lstFilteredQuestions.add(question);
            }
        }

        return lstFilteredQuestions;
    } //filterQuestions

    /**
     * Initialize a List of Question from the user's .csv filePath input
     * @return List of Questions found in input file
     * @throws IOException
     */
    private <T extends GenericModel> List<Question> initializeQuestions() throws IOException {
        List<Question> lstInputQuestions = new ArrayList<>();

        String row;
        BufferedReader csvReader = new BufferedReader(new FileReader(inputFilePath));

        /*  Instantiation of our NaturalLanguageUnderstandingModule to be sure that the categories and concepts analyzed
        in the typical correct answer an the user's answer are the same to the ones analyzed when interacting with the chatbot */
        NaturalLanguageUnderstandingModule nlu = NaturalLanguageUnderstandingModule.getInstance();

        /*  Each row must be as specified in documentation : 'THEME, question, typical correct answer, hint[optional]'   */
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(Constants.DATA_SEPARATOR);

            try {
                Theme theme = Theme.valueOf(data[0]);
                String question = data[1];
                String answer = data[2];

                Map<String, List<T>> lstAnalysis = nlu.setRecognizable(answer);
                List<CategoriesResult> categories = (List<CategoriesResult>) lstAnalysis.get(Constants.CATEGORIES_ID);
                List<ConceptsResult> concepts = (List<ConceptsResult>) lstAnalysis.get(Constants.CONCEPTS_ID);

                if (data.length > 3) {
                    String hint = data[4];
                    lstInputQuestions.add(new Question(theme, question, answer, hint, categories, concepts));
                } else {
                    lstInputQuestions.add(new Question(theme, question, answer, categories, concepts));
                }
                System.out.println("question added");
            } catch (IllegalArgumentException e) {
                System.out.println("you have entered a wrong Theme identifier : " + data[0] + " is not a theme handled by the EducativeChatbot (case-sensitive)");
                e.printStackTrace();
            }
        }
        csvReader.close();

        return lstInputQuestions;
    } //initializeQuestions

}
