package com.alura.literalura.principal;

import com.alura.literalura.model.*;
import com.alura.literalura.repository.AutorRepository;
import com.alura.literalura.service.ConsumoAPI;
import com.alura.literalura.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
private Scanner teclado = new Scanner(System.in);
private ConsumoAPI consumoAPI = new ConsumoAPI();
private ConvierteDatos conversor = new ConvierteDatos();
private String URL_BASE = "https://gutendex.com/books/";
private AutorRepository repository;

public Principal(AutorRepository repository){
    this.repository = repository;
}

public void mostrarMenu() {
    var opcion = -1;
    var menu = """
            ----------------MENU PRINCIPAL--------------
            1 - Buscar libros por tÍtulo
            2 - Listar libros registrados
            3 - Listar autores registrados
            4 - Listar autores vivos en un determinado año
            5 - Listar libros por idioma
            ----------------------------------------------
            0 - Salir del programa literalura
            ----------------------------------------------
            Selecciona una opción:
            ----------------------------------------------
            """;

    while (opcion != 0) {
        System.out.println(menu);
        try {
            opcion = Integer.valueOf(teclado.nextLine());
            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    listarAutoresVivos();
                    break;
                case 5:
                    listarLibrosPorIdioma();
                    break;

                case 0:
                    System.out.println("Saliendo de Literalura....");
                    break;
                default:
                    System.out.println("Opción inválida!");
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Opción inválida: " + e.getMessage());

        }
    }
}
public void buscarLibroPorTitulo() {
    System.out.println("""
                Busqueda de libros por título 
            --------------------------------------
             """);
    System.out.println("Introduzca el nombre del libro que desea buscar:");
    var nombre = teclado.nextLine();
    var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + nombre.replace(" ", "+").toLowerCase());

    // Se crea objeto JSON vacio
    if (json.isEmpty() || !json.contains("\"count\":0,\"next\":null,\"previous\":null,\"results\":[]")) {
        var datos = conversor.obtenerDatos(json, Datos.class);


        Optional<DatosLibro> libroBuscado = datos.libros().stream()
                .findFirst();
        if (libroBuscado.isPresent()) {
            System.out.println(
                    "\n------------- Libro --------------" +
                            "\nTítulo: " + libroBuscado.get().titulo() +
                            "\nAutor: " + libroBuscado.get().autores().stream()
                            .map(a -> a.nombre()).limit(1).collect(Collectors.joining()) +
                            "\nIdioma: " + libroBuscado.get().idiomas().stream().collect(Collectors.joining()) +
                            "\nNúmero de descargas: " + libroBuscado.get().descargas() +
                            "\n--------------------------------------\n"
            );

            try {
                List<Libro> libroEncontrado = libroBuscado.stream().map(a -> new Libro(a)).collect(Collectors.toList());
                Autor autorAPI = libroBuscado.stream().
                        flatMap(l -> l.autores().stream()
                                .map(a -> new Autor(a)))
                        .collect(Collectors.toList()).stream().findFirst().get();
                Optional<Autor> autorBD = repository.buscarAutorPorNombre(libroBuscado.get().autores().stream()
                        .map(a -> a.nombre())
                        .collect(Collectors.joining()));
                Optional<Libro> libroOptional = repository.buscarLibroPorNombre(nombre);
                if (libroOptional.isPresent()) {
                    System.out.println("El libro ya existe en la BD.");
                } else {
                    Autor autor;
                    if (autorBD.isPresent()) {
                        autor = autorBD.get();
                        System.out.println("El autor ya existe en la BD");
                    } else {
                        autor = autorAPI;
                        repository.save(autor);
                    }
                    autor.setLibros(libroEncontrado);
                    repository.save(autor);
                }
            } catch (Exception e) {
                System.out.println("Alerta! " + e.getMessage());
            }
        } else {
            System.out.println("El Libro que busca no fue encontrado");
        }
    }
}

public void listarLibrosRegistrados () {
            System.out.println("""
                      Lista de libros almacenados 
                    -------------------------------
                     """);
            List<Libro> libros = repository.buscarTodosLosLibros();
            libros.forEach(l -> System.out.println(
                    "-------------- Libro -----------------" +
                            "\nTítulo: " + l.getTitulo() +
                            "\nAutor: " + l.getAutor().getNombre() +
                            "\nIdioma: " + l.getIdioma().getIdioma() +
                            "\nNúmero de descargas: " + l.getDescargas() +
                            "\n----------------------------------------\n"
            ));
        }

        public void listarAutoresRegistrados () {
            System.out.println("""
                      Lista de Autores Registrados  
                    ----------------------------------
                     """);
            List<Autor> autores = repository.findAll();
            System.out.println();
            autores.forEach(l -> System.out.println(
                    "Autor: " + l.getNombre() +
                            "\nFecha de nacimiento: " + l.getNacimiento() +
                            "\nFecha de muerte: " + l.getMuerte() +
                            "\nLibros: " + l.getLibros().stream()
                            .map(t -> t.getTitulo()).collect(Collectors.toList()) + "\n"
            ));
        }

        public void listarAutoresVivos () {
            System.out.println("""
                      Lista de autores vivos
                    ---------------------------
                     """);
            System.out.println("Introduzca un año en busca de autor(es) vivos: ");
            try {
                var fecha = Integer.valueOf(teclado.nextLine());
                List<Autor> autores = repository.buscarAutoresVivos(fecha);
                if (!autores.isEmpty()) {
                    System.out.println();
                    autores.forEach(a -> System.out.println(
                            "Autor: " + a.getNombre() +
                                    "\nFecha de nacimiento: " + a.getNacimiento() +
                                    "\nFecha de muerte: " + a.getMuerte() +
                                    "\nLibros: " + a.getLibros().stream()
                                    .map(l -> l.getTitulo()).collect(Collectors.toList()) + "\n"
                    ));
                } else {
                    System.out.println("No hay autores vivos en el año que buscabas");
                }
            } catch (NumberFormatException e) {
                System.out.println("Ingresa un año válido " + e.getMessage());
            }
        }

    public void listarLibrosPorIdioma() {
        System.out.println("""
                  Lista de libros por idioma 
                --------------------------------
                """);
        var menu = """
                    ---------------------------------------------------
                    Seleccione el idioma del libro que desea buscar:
                    ---------------------------------------------------
                    1 - Español - (es)
                    2 - Francés - (fr)
                    3 - Inglés  - (en)
                    4 - Portugués - (pt)
                    ----------------------------------------------------
                    """;
        System.out.println(menu);

        try {
            var opcion = Integer.parseInt(teclado.nextLine());

            switch (opcion) {
                case 1:
                    buscarLibrosPorIdioma("es");
                    break;
                case 2:
                    buscarLibrosPorIdioma("fr");
                    break;
                case 3:
                    buscarLibrosPorIdioma("en");
                    break;
                case 4:
                    buscarLibrosPorIdioma("pt");
                    break;
                default:
                    System.out.println("Opción inválida...");
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Opción inválida: " + e.getMessage());
        }
    }

    private void buscarLibrosPorIdioma(String idioma) {
        try {
            Idioma idiomaEnum = Idioma.valueOf(idioma.toUpperCase());
            List<Libro> libros = repository.buscarLibrosPorIdioma(idiomaEnum);
            if (libros.isEmpty()) {
                System.out.println("No hay libros registrados en este idioma: "+idioma);
            } else {
                System.out.println();
                libros.forEach(l -> System.out.println(
                        "----------- Datos de Libro -----1------" +
                                "\nTítulo: " + l.getTitulo() +
                                "\nAutor: " + l.getAutor().getNombre() +
                                "\nIdioma: " + l.getIdioma().getIdioma() +
                                "\nNúmero de descargas: " + l.getDescargas() +
                                "\n----------------------------------------\n"
                ));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Idioma inválido o no soportado.");
        }
    }

}
