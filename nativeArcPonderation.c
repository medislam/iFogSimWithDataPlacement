#include <jni.h>
#include <stdio.h>
#include "org_fog_jni_GraphPonderation.h"
#include <float.h>
#include <omp.h>
#include <stdlib.h>
#include <limits.h>


#define Oriented 0 /* if oriented graph T <-1, else T<- 0 */

jint *LS, *LP, *F;


void printDist(jint n,jfloat **dis) {
	int i, j;

	printf("\t");
	for (i = 0; i < n; ++i)
		printf("\t%4c", 'A' + i);

	printf("\n");
	for (i = 0; i < n; ++i) {
		printf("\t%4c", 'A' + i);
		for (j = 0; j < n; ++j){
			if (dis[i][j]==FLT_MAX)
				printf("\t%4s", "Inf");
			else
				printf("\t%4.1f", dis[i][j]);
		}
		printf("\n");
	}
	printf("\n");
}


void printFlow(jint n,jint **flw) {
	int i, j;

	printf("\t");
	for (i = 0; i < n; ++i)
		printf("\t%4c", 'A' + i);

	printf("\n");
	for (i = 0; i < n; ++i) {
		printf("\t%4c", 'A' + i);
		for (j = 0; j < n; ++j){
			if (flw[i][j]==INT_MAX)
				printf("\t%4s", "Inf");
			else
				printf("\t%4d", flw[i][j]);
		}
		printf("\n");
	}
	printf("\n");
}


void print_table(jint t, jint* tab){
	jint i;
	for (i = 0; i < t; ++i){
		printf("%d%s\t",tab[i],",");
	}
}


void init_LP_LS(jint n, jint nb_source_vertice, jint nb_arc, jfloat** dis){
	jint i,j,indx_ls;

	printf("n = %d,  nb_source_vertice = %d, nb_arc = %d \n",n, nb_source_vertice, nb_arc );
	LP = malloc(nb_source_vertice*sizeof(jint));
	LS = malloc(nb_arc*sizeof(jint));

	indx_ls=0,i=0;
	/* End of index in LP*/
	LP[nb_source_vertice-1]=nb_arc;

	while(i < n && indx_ls < nb_arc){
		LP[i]=indx_ls;
		// non-oriented Graph
		for(j=i+1;j<n;j++){

			if(dis[i][j]!=FLT_MAX){
				LS[indx_ls]=j;
				++indx_ls;
			}
		}
		++i;
	}
}


void increment_arc(jint src, jint dst){
	jint kmin=-1, kmax=-1, u=-1;
	if(!Oriented){
		kmin = src <dst ? src :dst;
		kmax = src >=dst? src : dst;
	}else{
		kmin=src;
		kmax=dst;
	}

	for(u=LP[kmin];u<LP[kmin+1];u++)
		if(LS[u] == kmax) 
			break;
	#pragma omp atomic
	F[u]++;	
}

void increment_flow(jint** flow, int i, jint dst){
	jint src;

	//while not arrives to the original source 'i' do:
	while (dst!=i){
		//get the source of the arc && increment the arc && the next destination is the past source
		src=flow[i][dst];
		//printf(" increment_arc(%d,%d)|\t",src,dst);
		increment_arc(src,dst);
		dst=src;
	}
}

void arc_ponderation(jint n, jint* LP_prod2, jint* LS_cons2, jint** flow){
	int i;

	printf("\n");
	#pragma omp parallel for private(i)
	for(i=0; i<n;i++){
		int index_bgn = LP_prod2[i];
		int index_end = LP_prod2[i+1];
		int index;
		//printf("index_bgn = %d,  index_end = %d \t\t", index_bgn, index_end);
		for(index = index_bgn; index<index_end; index++){
			if(LS_cons2[index]==-1){
				//printf("Node %d hasn't any consumer",i);
				continue;
			}

			jint dst;
			dst = LS_cons2[index];

			if(flow[i][dst]==INT_MAX){
				printf("Error, there is no link betweek source:%d and destination:%d", i,dst);
				exit(0);
			} 

			//printf("|||increment flow %d <--> %d  ", i, dst );

			increment_flow(flow, i, dst);	
		}
		//printf("\n");
	}
}


JNIEXPORT jintArray JNICALL  Java_org_fog_jni_GraphPonderation_nativeArcPoderation(JNIEnv *env, jobject obj,\
								 jint LP_pord_Length, jint LS_cons_Length, jintArray LP_pord, jintArray LS_cons,\
								 jint LPLength, jint LSLength, jobjectArray dist, jobjectArray flow){

	int i, nb_source_vertice=LPLength, nb_arc=LSLength;
	int n = LP_pord_Length, m = LS_cons_Length;
	jint** localArray;
	jintArray* oneDim; 

	jfloat** localArray2;
	jfloatArray* oneDim2;
	jint* LP_pord2 = malloc(n*sizeof(jint));
	jint* LS_cons2 = malloc(m*sizeof(jint));
	jintArray result;

	F = calloc(nb_arc,sizeof(jint));
	

	LP_pord2 = (*env)->GetIntArrayElements(env,LP_pord,NULL);
	LS_cons2 = (*env)->GetIntArrayElements(env,LS_cons,NULL);

	//n -> nombre de noeuds+1, m-> nombre d'arcs et flow -> la matrice de plus court chemin
	//recupération des données de plus court chemain

	oneDim = malloc((n-1)*sizeof(jintArray*));
	localArray = malloc((n-1)*sizeof(jint*));

	for (i = 0; i < n-1; ++i){
		oneDim[i] = (*env)->GetObjectArrayElement(env,flow,i);
		localArray[i] = (*env)->GetIntArrayElements(env,oneDim[i],NULL);
	}


	/* recupération de la matrice des distances (Float)*/
	oneDim2 = malloc((n-1)*sizeof(jfloatArray*));
	localArray2 = malloc((n-1)*sizeof(jfloat*));

	for (i = 0; i < n-1; ++i){
		oneDim2[i] = (*env)->GetObjectArrayElement(env,dist,i);
		localArray2[i] = (*env)->GetFloatArrayElements(env,oneDim2[i],NULL);
	}


	//printf("\t%s\n","---------------- dist ------------------------" );
	//printDist(n-1,localArray2);

	//printf("\t%s\n","---------------- flow ------------------------" );
	//printFlow(n-1,localArray);

	//printf("\n\t---------------- LP_pord Table ----------------\n");
	//print_table(n, LP_pord2);

	//printf("\n\t---------------- LS_cons Table ---------------- \n");
	//print_table(m, LS_cons2);

	//printf("\nnb_source_vertice=%d\n",nb_source_vertice);
	//printf("nb_arc=%d\n",nb_arc);

	//printf("1\n");
	init_LP_LS(n-1, nb_source_vertice, nb_arc, localArray2);

	//printf("\n\n\t---------------- LP Table ----------------\n");
	//print_table(nb_source_vertice, LP);

	//printf("\n\n\t---------------- LS Table ---------------- \n");
	//print_table(nb_arc, LS);
	//printf("\n\n\n");

	//printf("2\n");
	arc_ponderation(n-1, LP_pord2, LS_cons2, localArray);

	//printf("\n\n\t---------------- F Table ---------------- \n");
	//print_table(nb_arc, F);

	//printf("\n");
	//printf("\n!!!!nb_arc = %d\n",nb_arc );
	result = (*env)->NewIntArray(env,nb_arc);
	(*env)->SetIntArrayRegion(env, result, 0 , nb_arc , F);

	printf("Free local tables ...\n");

	free(oneDim);
	printf("oneDim is free\n");
	printf("Pointeur of oneDim:%p\n",oneDim );

	free(oneDim2);
	printf("oneDim2 is free\n");


	free(localArray);
	printf("localArray is free\n");

	free(localArray2);
	printf("localArray2 is free\n");

	free(F);
	printf("F is free\n");

	free(LP_pord2);
	printf("LP_pord2 is free\n");

	free(LS_cons2);
	printf("LS_cons2 is free\n");

	printf("\nEND of arc ponderation!\n");
	return result;
}