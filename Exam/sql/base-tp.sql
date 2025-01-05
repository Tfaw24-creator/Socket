CREATE DATABASE Noel;
USE Noel;
CREATE TABLE Noel_Admin (
    id_admin INT AUTO_INCREMENT PRIMARY KEY,
    Nom VARCHAR(255) NOT NULL,
    Pwd VARCHAR(255) NOT NULL
);
CREATE TABLE Noel_Utilisateur (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    nom_user VARCHAR(255) NOT NULL,
    pwd_user VARCHAR(255) NOT NULL
);
CREATE TABLE Noel_Depot (
    id_depot INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT,
    Status INT DEFAULT 0,
    Montant DECIMAL(10, 2),
    FOREIGN KEY (id_user) REFERENCES Noel_Utilisateur(id_user)
);
CREATE TABLE Noel_Enfant (
    id_enfant INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT,
    nom_enfant VARCHAR(255),
    prenom_enfant VARCHAR(255),
    genre_enfant TINYINT CHECK (genre_enfant BETWEEN 0 AND 2),
    FOREIGN KEY (id_user) REFERENCES Noel_Utilisateur(id_user)
);
CREATE TABLE Noel_Cadeau (
    id_cadeau INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT,
    genre_cadeau TINYINT CHECK (genre_cadeau BETWEEN 0 AND 2),
    nom_cadeau VARCHAR(255),
    FOREIGN KEY (id_user) REFERENCES Noel_Utilisateur(id_user)
);