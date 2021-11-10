# Angular UI

This project was generated with [Angular CLI](https://cli.angular.io/) version 8.3.5.

## Setup

Make sure you have [Node JS](https://nodejs.org) installed and [Angular CLI](https://github.com/angular/angular-cli)

```
$ ng version

Angular CLI: 8.3.5
Node: 12.10.0
```

If you need to update the version of Angular CLI you can run

``` 
npm install -g @angular/cli
```

## Angular CLI commands

### Initiliaze

```
$ ng new super-heroes --directory super-heroes-ui --prefix hero --routing false --skipTests true --inlineStyle true --commit false --minimal true --style css
```

### Admin components

```
$ ng generate component fight-list --spec false --inline-style true
$ ng generate component fight --spec false --inline-style true
```

### Material design

```
$ ng add @angular/material
```

### Swagger Codegen

```
$ swagger-codegen generate -i http://localhost:8082/openapi -l typescript-angular -o src/app/shared
```



## Angular CLI documentation

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
