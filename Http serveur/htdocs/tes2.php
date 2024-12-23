<?php
// Indiquer que la réponse est en format JSON
header('Content-Type: application/json');

// Vérifier si la requête est de type POST
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    // Récupérer les données JSON envoyées dans la requête
    $data = json_decode(file_get_contents("php://input"), true);

    // Vérifier si les données ont été correctement récupérées
    if (isset($data['nom']) && isset($data['email'])) {
        $nom = $data['nom'];
        $email = $data['email'];

        // Simuler un traitement avec les données reçues
        $response = array(
            'status' => 'success',
            'message' => 'Données reçues et traitées avec succès',
            'nom' => $nom,
            'email' => $email
        );
    } else {
        // Si les données sont incomplètes
        $response = array(
            'status' => 'error',
            'message' => 'Les données sont incomplètes'
        );
    }

    // Retourner la réponse au format JSON
    echo json_encode($response);
} else {
    // Si ce n'est pas une requête POST
    $response = array(
        'status' => 'error',
        'message' => 'Méthode non autorisée, seule POST est autorisée'
    );
    echo json_encode($response);
}
?>
