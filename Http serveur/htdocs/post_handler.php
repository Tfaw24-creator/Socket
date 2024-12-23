<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Formulaire PHP avec POST</title>
</head>
<body>
    <?php
    // Vérifier si le formulaire a été soumis
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        // Récupérer et sécuriser les données du formulaire
        $nom = htmlspecialchars($_POST['nom'] ?? '');
        $email = htmlspecialchars($_POST['email'] ?? '');
        $message = htmlspecialchars($_POST['message'] ?? '');

        // Validation simple
        if (!empty($nom) && filter_var($email, FILTER_VALIDATE_EMAIL) && !empty($message)) {
            echo "<h2>Merci, $nom !</h2>";
            echo "<p>Nous avons reçu votre message :</p>";
            echo "<blockquote>" . nl2br($message) . "</blockquote>";
            echo "<p>Nous vous répondrons à l'adresse : <strong>$email</strong></p>";
        } else {
            echo "<p style='color: red;'>Veuillez remplir tous les champs correctement.</p>";
        }
    } else {
        // Afficher un message d'accueil
        echo "<h2>Bienvenue ! Veuillez remplir le formulaire ci-dessous.</h2>";
    }
    ?>

    <!-- Formulaire HTML -->
    <form method="POST" action="">
        <label for="nom">Nom :</label><br>
        <input type="text" id="nom" name="nom" required><br><br>

        <label for="email">Email :</label><br>
        <input type="email" id="email" name="email" required><br><br>

        <label for="message">Message :</label><br>
        <textarea id="message" name="message" rows="5" required></textarea><br><br>

        <button type="submit">Envoyer</button>
    </form>
</body>
</html>
